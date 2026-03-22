package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final UserClient userClient;

    private final IInteractionReplyService interactionReplyService;

    private final SearchClient searchClient;

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final CategoryCache categoryCache;

    /**
     * 保存互动问题
     *
     * @param question 问题表单数据传输对象，包含问题内容、类型等信息
     */
    @Override
    public void saveQuestion(QuestionFormDTO question) {
        //获取登录用户
        Long userId = UserContext.getUser();
        //保存数据
        InteractionQuestion interactionQuestion = BeanUtil.toBean(question, InteractionQuestion.class);
        interactionQuestion.setUserId(userId);
        this.save(interactionQuestion);
    }


    /**
     * 分页查询互动问题列表
     * <p>
     * 该方法实现互动问题的分页查询功能，支持按课程、章节过滤，并处理用户信息和最新回答信息的封装。
     * 查询时会排除被隐藏的问题，并根据需要获取提问者和回答者的用户信息。
     *
     * @param query 问题分页查询条件，包含课程ID、章节ID、是否仅查询当前用户等问题筛选条件
     * @return PageDTO<QuestionVO> 返回分页的QuestionVO对象，包含问题基本信息、提问者信息及最新回答信息
     * @throws BadRequestException 当课程ID和章节ID同时为空时抛出异常，防止全表查询
     */
    @Override
    public PageDTO<QuestionVO> queryQuestionsForPage(QuestionPageQuery query) {
        //参数效验。避免全表查询
        if (query.getCourseId() == null && query.getSectionId() == null) {
            throw new BadRequestException("课程Id和章节Id不能同时为空");
        }

        //分页从问题表筛选出数据
        Long userId = UserContext.getUser();
        Page<InteractionQuestion> questionPage = this.lambdaQuery()
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, userId)
                .eq(query.getCourseId() != null, InteractionQuestion::getCourseId, query.getCourseId())
                .eq(query.getSectionId() != null, InteractionQuestion::getSectionId, query.getSectionId())
                .eq(InteractionQuestion::getHidden, false)
                .select(InteractionQuestion.class, tableFieldInfo -> !tableFieldInfo.getColumn().equals("description"))
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = questionPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(questionPage);
        }

        // 获取非匿名用户的提问者信息集合
        Set<Long> userIdSet = records.stream()
                .filter(interactionQuestion -> interactionQuestion.getAnonymity().equals(false) && interactionQuestion.getUserId() != null)
                .map(InteractionQuestion::getUserId)
                .collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(userIdSet)) {
            userClient.queryUserByIds(userIdSet);
        }

        // 获取最近一次回答ID集合
        Set<Long> recentAnswerIdSet = records.stream()
                .map(InteractionQuestion::getLatestAnswerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 获取非隐藏的回答数据并建立ID映射
        Map<Long, InteractionReply> replyMap = interactionReplyService.listByIds(recentAnswerIdSet)
                .stream()
                .filter(interactionReply -> interactionReply.getHidden().equals(false))
                .collect(Collectors.toMap(InteractionReply::getId, interactionReply -> interactionReply));
        replyMap.forEach((key, value) -> {
            if (value.getAnonymity().equals(false)) {
                userIdSet.add(value.getUserId());
            }
        });

        // 批量查询用户信息并建立ID映射
        Map<Long, UserDTO> userDTOMapById = userClient.queryUserByIds(userIdSet)
                .stream()
                .collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));

        // 封装结果数据，包括问题信息、用户信息和最新回答信息
        List<QuestionVO> collect = records.stream().map(interactionQuestion -> {
            QuestionVO questionVO = BeanUtil.toBean(interactionQuestion, QuestionVO.class);
            UserDTO userInfo = null;
            if (interactionQuestion.getUserId() != null) {
                userInfo = userDTOMapById.get(interactionQuestion.getUserId());
            }
            InteractionReply interactionReplyInfo = null;
            if (interactionQuestion.getLatestAnswerId() != null) {
                interactionReplyInfo = replyMap.get(interactionQuestion.getLatestAnswerId());
            }
            if (userInfo != null && !interactionQuestion.getAnonymity()) {
                questionVO.setUserName(userInfo.getUsername());
                questionVO.setUserIcon(userInfo.getIcon());
            }
            if (interactionReplyInfo != null && interactionQuestion.getLatestAnswerId() != null) {
                UserDTO userDTO = userDTOMapById.get(interactionReplyInfo.getUserId());
                if (userDTO != null && interactionReplyInfo.getAnonymity().equals(false)) {
                    questionVO.setLatestReplyUser(userDTO.getName());
                }
                questionVO.setLatestReplyContent(interactionReplyInfo.getContent());
            }
            return questionVO;
        }).collect(Collectors.toList());

        return PageDTO.of(questionPage, collect);
    }


    /**
     * 根据ID查询问题详情
     * @param id 问题ID
     * @return QuestionVO 问题视图对象，如果问题不存在或被隐藏则返回null
     */
    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 查询出未被隐藏的问题
        InteractionQuestion question = getById(id);
        if (question == null || question.getHidden()){
            log.warn("问题不存在或被隐藏");
            return null;
        }
        
        // 封装问题基本信息到VO
        QuestionVO questionVO = BeanUtil.toBean(question, QuestionVO.class);
        // 处理非匿名问题的用户信息
        if(!question.getAnonymity()){
            UserDTO userDTO = userClient.queryUserById(question.getUserId());
            questionVO.setUserName(userDTO != null? userDTO.getName():null);
            questionVO.setUserIcon(userDTO != null?userDTO.getIcon():null);
        }
        
        // 返回封装好的问题VO
        return questionVO;
    }


    /**
     * 分页查询后台管理问题列表
     * 
     * @param query 查询条件对象，包含课程名称、状态、时间范围等筛选条件
     * @return PageDTO<QuestionAdminVO> 分页结果，包含问题详情及关联的课程、章节、用户等信息
     */
    public PageDTO<QuestionAdminVO> queryAdminQuestionsForPage(QuestionAdminPageQuery query) {
        // 根据课程名称查询对应的课程ID列表
        List<Long> courseIdList = null;
        if(StrUtil.isNotEmpty(query.getCourseName())){
            List<Long> longs = searchClient.queryCoursesIdByName(query.getCourseName());
            if(CollUtil.isEmpty(longs)){
                log.warn("根据课程名称未查询到课程ID");
                return PageDTO.empty(0L,0L);
            }
            courseIdList = longs;
        }

        // 执行分页查询，根据各种条件筛选问题
        Page<InteractionQuestion> questionPage = this.lambdaQuery()
                .in(CollUtil.isNotEmpty(courseIdList), InteractionQuestion::getCourseId, courseIdList)
                .eq(query.getStatus() != null, InteractionQuestion::getStatus, query.getStatus())
                .gt(query.getBeginTime() != null, InteractionQuestion::getCreateTime, query.getBeginTime())
                .lt(query.getEndTime() != null, InteractionQuestion::getCreateTime, query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = questionPage.getRecords();
        if(CollUtil.isEmpty(records)){
            log.debug("未查询到任何数据");;
            return PageDTO.empty(questionPage);
        }

        // 收集所有需要关联查询的ID集合（章节、课程、用户）
        Set<Long> cataIdSet = new HashSet<>(records.size()*2);
        Set<Long> courseIdSet = new HashSet<>(records.size());
        Set<Long> userIdSet = new HashSet<>(records.size());
        records.forEach(interactionQuestion -> {
            if(null != interactionQuestion.getChapterId()){
                cataIdSet.add(interactionQuestion.getChapterId());
            }
            if(null != interactionQuestion.getSectionId()){
                cataIdSet.add(interactionQuestion.getSectionId());
            }
            if(null != interactionQuestion.getCourseId()){
                courseIdSet.add(interactionQuestion.getCourseId());
            }
            if(null != interactionQuestion.getUserId()){
                userIdSet.add(interactionQuestion.getUserId());
            }
        });

        // 批量查询关联的实体数据
        List<CataSimpleInfoDTO> cataSimpleInfoDTOList = catalogueClient.batchQueryCatalogue(cataIdSet);
        List<CourseSimpleInfoDTO> courseSimpleInfoDTOList = courseClient.getSimpleInfoList(courseIdSet);
        List<UserDTO> userDTOList = userClient.queryUserByIds(userIdSet);

        // 转换为Map以便快速查找
        Map<Long, String> cataSimpleInfoDTOMap = null;
        if(CollUtil.isNotEmpty(cataSimpleInfoDTOList)){
            cataSimpleInfoDTOMap = cataSimpleInfoDTOList.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        Map<Long, CourseSimpleInfoDTO> courseSimpleInfoDTOMap = null;
        if(CollUtil.isNotEmpty(courseSimpleInfoDTOList)){
            courseSimpleInfoDTOMap = courseSimpleInfoDTOList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }
        Map<Long, UserDTO> userDTOMap = null;
        if(CollUtil.isNotEmpty(userDTOList)){
            userDTOMap = userDTOList.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        }

        // 遍历查询结果，封装成VO对象
        ArrayList<QuestionAdminVO> questionAdminVOS = new ArrayList<>(records.size());
        for(InteractionQuestion question : records) {
            QuestionAdminVO questionAdminVO = BeanUtil.toBean(question, QuestionAdminVO.class);
            if (CollUtil.isNotEmpty(cataSimpleInfoDTOMap)) {
                questionAdminVO.setChapterName(cataSimpleInfoDTOMap.getOrDefault(question.getChapterId(),null));
                questionAdminVO.setSectionName(cataSimpleInfoDTOMap.getOrDefault(question.getSectionId(),null));
            }
            if(CollUtil.isNotEmpty(courseSimpleInfoDTOMap) && courseSimpleInfoDTOMap.get(question.getCourseId()) !=null){
                CourseSimpleInfoDTO courseSimpleInfoDTO = courseSimpleInfoDTOMap.get(question.getCourseId());
                List<Long> cateIdList = List.of(courseSimpleInfoDTO.getFirstCateId(), courseSimpleInfoDTO.getSecondCateId(), courseSimpleInfoDTO.getThirdCateId());
                String categoryNames = categoryCache.getCategoryNames(cateIdList);
                questionAdminVO.setCourseName(courseSimpleInfoDTO.getName());
                questionAdminVO.setCategoryName(categoryNames);
            }
            if(CollUtil.isNotEmpty(userDTOMap) && userDTOMap.get(question.getUserId()) != null){
                questionAdminVO.setUserIcon(userDTOMap.get(question.getUserId()).getIcon());
                questionAdminVO.setUserName(userDTOMap.get(question.getUserId()).getName());
            }
            questionAdminVOS.add(questionAdminVO);
        }

        return PageDTO.of(questionPage,questionAdminVOS);
    }

    /**
     * 查询管理端问题详情
     * @param id
     * @return
     */
    @Override
    public QuestionAdminVO queryQuestionAdminById(Long id) {
        //查询问题
        InteractionQuestion question1 = this.getById(id);
        if (question1 == null) {
            log.warn("根据id未查询到问题");
            return null;
        }

        //获取其他字段的信息
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(List.of(question1.getChapterId(), question1.getSectionId()));
        Map<Long, CataSimpleInfoDTO> cataSimpleInfoDTOMap = null;
        if(CollUtil.isNotEmpty(cataSimpleInfoDTOS)){
            cataSimpleInfoDTOMap = cataSimpleInfoDTOS.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, cata -> cata));

        }
        CourseFullInfoDTO courseInfoById = courseClient.getCourseInfoById(question1.getCourseId(), false, true);
        HashSet<Long> userIdSet = new HashSet<>();
        userIdSet.add(question1.getUserId());
        if (courseInfoById != null && CollUtil.isNotEmpty(courseInfoById.getTeacherIds())) {
            userIdSet.addAll(courseInfoById.getTeacherIds());
        }
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIdSet);
        Map<Long, UserDTO> userDTOMap = null;
        if(CollUtil.isNotEmpty(userDTOS)){
            userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));
        }
        String categoryNames = null;
        if(null != courseInfoById){
            categoryNames = categoryCache.getCategoryNames(List.of(courseInfoById.getFirstCateId(), courseInfoById.getSecondCateId(), courseInfoById.getThirdCateId()));
        }

        //封装返回数据
        QuestionAdminVO questionAdminVO = BeanUtil.toBean(question1, QuestionAdminVO.class);
        if(userDTOMap != null){
            if(userDTOMap.get(question1.getUserId()) != null){
                questionAdminVO.setUserName(userDTOMap.get(question1.getUserId()).getName());
            }
            if(userDTOMap.get(question1.getUserId()) != null){
                questionAdminVO.setUserIcon(userDTOMap.get(question1.getUserId()).getIcon());
            }
            if (courseInfoById != null) {
                ArrayList<String> teacherNameList = new ArrayList<String>(courseInfoById.getTeacherIds().size());
                for (Long teacherId : courseInfoById.getTeacherIds()) {
                    teacherNameList.add(userDTOMap.get(teacherId).getName());
                }
                String teacherNames = CollUtil.join(teacherNameList, "/");
                questionAdminVO.setTeacherName(teacherNames);
            }
        }
        if(cataSimpleInfoDTOMap != null){
            if( cataSimpleInfoDTOMap.get(question1.getChapterId()) != null){
                questionAdminVO.setChapterName(cataSimpleInfoDTOMap.get(question1.getChapterId()).getName());
            }
            if(null != cataSimpleInfoDTOMap.get(question1.getSectionId())){
                questionAdminVO.setSectionName(cataSimpleInfoDTOMap.get(question1.getSectionId()).getName());
            }
        }
        if (courseInfoById != null) {
            questionAdminVO.setCourseName(courseInfoById.getName());
        }
        questionAdminVO.setCategoryName(categoryNames);

        //修改问题查看状态
        if(question1.getStatus() == QuestionStatus.UN_CHECK){
            InteractionQuestion question = new InteractionQuestion().setId(question1.getId()).setStatus(QuestionStatus.CHECKED);
            this.updateById(question);
        }
        return questionAdminVO;
    }
}
