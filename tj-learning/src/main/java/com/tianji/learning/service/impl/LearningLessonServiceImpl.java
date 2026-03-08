package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSearchDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-01-18
 */
@Service
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final ILearningRecordService iLearningRecordService;


    /**
     * 构造方法，注入学习课程服务所需的依赖组件
     * @Lazy(解决循环依赖问题)： 当标记为延迟加载时，Spring 会注入一个代理对象，而不是真正的目标 Bean。第一次使用代理时，代理会触发目标 Bean 的加载，并将调用委托给真实对象。
     * @param courseClient 课程客户端，用于远程调用课程相关服务
     * @param catalogueClient 目录客户端，用于远程调用课程目录相关服务
     * @param iLearningRecordService 学习记录服务，使用@Lazy 注解延迟加载，用于处理学习记录相关业务
     */
    public LearningLessonServiceImpl(CourseClient courseClient,
                                     CatalogueClient catalogueClient,
                                     @Lazy ILearningRecordService iLearningRecordService) {
        this.courseClient = courseClient;
        this.catalogueClient = catalogueClient;
        this.iLearningRecordService = iLearningRecordService;
    }

    /**
     * 添加用户购买的学习课程到课程表
     *
     * @param userId
     * @param courseIds
     */
    @Override
    @Transient
    public void addLessons(Long orderId, Long userId, List<Long> courseIds) {
        //获取课程过期时间
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(simpleInfoList)) {
            log.error("MQ消费者为{}订单{}用户添加课程{}失败：系统中无此课程ID", orderId, userId, courseIds);
            return;
        }

        //封装课程表数据集合
        List<LearningLesson> lessonList = simpleInfoList.stream().map(courseSimpleInfoDTO -> {
            LearningLesson learningLesson = new LearningLesson();
            Integer expiryTime = courseSimpleInfoDTO.getValidDuration();
            if (null != expiryTime && 0 < expiryTime) {
                learningLesson.setExpireTime(LocalDateTime.now().plusMonths(expiryTime));
            }
            learningLesson.setUserId(userId)
                    .setCourseId(courseSimpleInfoDTO.getId())
                    .setCreateTime(LocalDateTime.now());
            return learningLesson;
        }).collect(Collectors.toList());

        //新增课程到数据库
        this.saveBatch(lessonList);
    }


    /**
     * 获取我的课程
     *
     * @return
     */
    @Override
    public PageDTO<LearningLessonVO> getMyLessons(PageQuery pageQuery) {
        //获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null) {
            log.error("用户未登录");
            throw new BadRequestException("请先登录系统");
        }

        //获取用户课程表
        Page<LearningLesson> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        if (StringUtils.isEmpty(pageQuery.getSortBy())) {
            page.addOrder(OrderItem.desc("latest_learn_time"));
        } else {
            page.addOrder(new OrderItem(pageQuery.getSortBy(), pageQuery.getIsAsc()));
        }
        Page<LearningLesson> paged = this.lambdaQuery().eq(LearningLesson::getUserId, userId).page(page);

        //封装VO
        List<LearningLesson> records = paged.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(paged);
        }
        List<LearningLessonVO> learningLessonVOS = BeanUtil.copyToList(records, LearningLessonVO.class);
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        Map<Long, CourseSimpleInfoDTO> collect = courseClient.getSimpleInfoList(cIds).stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, courseSimpleInfoDTO -> courseSimpleInfoDTO, (a, b) -> a));
        learningLessonVOS.forEach(learningLessonVO -> {
            String courseName = collect.get(learningLessonVO.getCourseId()).getName();
            String coverUrl = collect.get(learningLessonVO.getCourseId()).getCoverUrl();
            Integer sectionNum = collect.get(learningLessonVO.getCourseId()).getSectionNum();
            learningLessonVO.setCourseName(courseName);
            learningLessonVO.setCourseCoverUrl(coverUrl);
            learningLessonVO.setSections(sectionNum);
        });

        return PageDTO.of(paged, learningLessonVOS);
    }

    /**
     * 获取用户最近学习课程
     *
     * @return
     */
    @Override
    public LearningLessonVO getRecentLearning() {
        //获取登录用户非未学习状态下的最近学习课程
        Long userId = UserContext.getUser();
        LearningLesson learningLesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .ne(LearningLesson::getStatus, LessonStatus.NOT_BEGIN)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 0,1")
                .one();
        if (null == learningLesson) {
            log.error("用户{}:无最近正在学习的课程", userId);
            return null;
        }
        LearningLessonVO learningLessonVO = BeanUtils.copyBean(learningLesson, LearningLessonVO.class);

        //远程调用获取课程相关信息
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(Collections.singleton(learningLesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataSimpleInfoDTOS)) {
            CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOS.get(0);
            learningLessonVO.setLatestSectionName(cataSimpleInfoDTO.getName());
            learningLessonVO.setLatestSectionIndex(cataSimpleInfoDTO.getCIndex());
        }

        //远程调用获取该课程正在学习的章节目录信息
        CourseSearchDTO searchInfo = courseClient.getSearchInfo(learningLessonVO.getCourseId());
        learningLessonVO.setCourseName(searchInfo.getName());
        learningLessonVO.setCourseCoverUrl(searchInfo.getCoverUrl());
        learningLessonVO.setSections(searchInfo.getSections());
        Long count = lambdaQuery().eq(LearningLesson::getUserId, userId).count();
        learningLessonVO.setCourseAmount(Math.toIntExact(count));

        return learningLessonVO;
    }


    /**
     * MQ用户退款删除用户课程
     *
     * @param orderBasicDTO
     */
    @Override
    @Transactional
    public void removeLessonOfRefund(OrderBasicDTO orderBasicDTO) {
        Long userId = orderBasicDTO.getUserId();
        List<Long> courseIds = orderBasicDTO.getCourseIds();
        LambdaQueryChainWrapper<LearningLesson> wrapper = this.lambdaQuery().eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds);
        boolean remove = this.remove(wrapper);
        if (!remove) {
            log.warn("用户退款删除其课程失败:{}", JSONUtil.toJsonStr(orderBasicDTO));
        }
    }

    /**
     * MQ课程删除用户课程
     *
     * @param courseId
     */
    @Override
    public void removeLessonById(String courseId) {
        //查询预删除的课程状态
        LearningLesson learningLesson = this.getById(courseId);
        if (learningLesson == null || !learningLesson.getStatus().equalsValue(LessonStatus.EXPIRED.getValue()) || learningLesson.getExpireTime().isAfter(LocalDateTime.now())) {
            log.error("删除课程失败(课程表ID):{}", courseId);
            throw new BadRequestException("该课程无法删除！");
        }
        this.removeById(courseId);
    }


    /**
     * 校验当前用户是否可以学习当前课程
     *
     * @param courseId
     * @return
     */
    @Override
    public Long checkCourseValid(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson learningLesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId).eq(LearningLesson::getCourseId, courseId).one();
        if (null == learningLesson || learningLesson.getStatus().equalsValue(LessonStatus.EXPIRED.getValue())) {
            log.error("课程不存在或已失效:{}", JSONUtil.toJsonStr(learningLesson));
            return null;
        }
        return learningLesson.getId();
    }

    /**
     * 获取用户课程状态
     *
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonVO getLessonStatus(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson learningLesson = this.lambdaQuery().eq(LearningLesson::getUserId, userId).eq(LearningLesson::getCourseId, courseId).one();
        if (null == learningLesson) {
            log.info("用户{}课表中无此课程:{}", userId, courseId);
            return null;
        }
        return BeanUtils.copyBean(learningLesson, LearningLessonVO.class);
    }

    /**
     * 获取课程学习人数
     *
     * @param courseId
     * @return
     */
    @Override
    public Integer getLearningCountById(Long courseId) {
        Long count = this.lambdaQuery().eq(LearningLesson::getCourseId, courseId).count();
        return Math.toIntExact(count);
    }


    /**
     * 更新课表信息
     * @param learningRecordFormDTO
     * @param learningLesson
     */
    @Override
    public void updateLearningInfo(LearningRecordFormDTO learningRecordFormDTO, LearningLesson learningLesson) {
        CourseSearchDTO clientSearchInfo = courseClient.getSearchInfo(learningLesson.getCourseId());
        assert null != clientSearchInfo && clientSearchInfo.getSections() != null;
        learningLesson.setLearnedSections(learningLesson.getLearnedSections() + 1)
                .setLatestSectionId(learningRecordFormDTO.getSectionId())
                .setLatestLearnTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime())
                .setStatus(learningLesson.getLearnedSections() + 1 >= clientSearchInfo.getSections() ? LessonStatus.FINISHED : null);
        boolean result = this.updateById(learningLesson);
        if (!result) {
            throw new DbException("更新课表信息失败");
        }
    }


    /**
     * 创建用户的学习计划
     * 设置学习频率并启动学习计划状态
     *
     * @param learningPlanDTO 学习计划数据传输对象，包含课程 ID 和学习频率信息
     */
    @Override
    public void createLearningPlan(LearningPlanDTO learningPlanDTO) {
        Long userId = UserContext.getUser();
        LearningLessonVO lessonStatus = this.getLessonStatus(learningPlanDTO.getCourseId());
        if(null == lessonStatus){
            throw  new BadRequestException("用户尚未购买此课程，创建计划失败");
        }
        LambdaUpdateWrapper<LearningLesson> lessonLambdaUpdateWrapper = new LambdaUpdateWrapper<LearningLesson>()
                .set(LearningLesson::getWeekFreq, learningPlanDTO.getFreq())
                .set(lessonStatus.getPlanStatus() == PlanStatus.NO_PLAN, LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, learningPlanDTO.getCourseId());
        this.update(lessonLambdaUpdateWrapper);
    }


    @Override
    public LearningPlanPageVO getCoursePlans(PageQuery pageQuery) {
        // 分页查询出当前用户所有计划的课程
        IPage<LearningLesson> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<LearningLesson>()
                .eq(LearningLesson::getUserId, UserContext.getUser())
                .in(LearningLesson::getStatus,Set.of(LessonStatus.NOT_BEGIN,LessonStatus.LEARNING))
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING);
        this.page(page, wrapper);
        List<LearningLesson> learningLessonList = page.getRecords();
        if(null == learningLessonList){
            return new LearningPlanPageVO();
        }

        //获取每个课程的本周已学习章节数
        Set<Long> LessonIdSet = learningLessonList.stream().map(LearningLesson::getId).collect(Collectors.toSet());
        List<LearningRecord> learningRecordList = iLearningRecordService.queryLearningRecordByLessonIds(LessonIdSet);
        Map<Long, Long> lessonCount = new HashMap<>();
        if(CollUtils.isNotEmpty(learningRecordList)){
            lessonCount = learningRecordList.stream().collect(Collectors.groupingBy(LearningRecord::getLessonId, Collectors.counting()));
        }

        //获取课程信息
        Set<Long> courseIdSet = learningLessonList.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        Map<Long, CourseSimpleInfoDTO> simpleInfoDTOMap = courseClient.getSimpleInfoList(courseIdSet).stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, myself -> myself));

        //封装课表计划数据
        Map<Long, Long> finalLessonCount = lessonCount;
        List<LearningPlanVO> learningPlanVOList = learningLessonList.stream().map(learningLesson -> {
            LearningPlanVO learningPlanVO = BeanUtil.copyProperties(learningLesson, LearningPlanVO.class);
            CourseSimpleInfoDTO courseSimpleInfoDTO = simpleInfoDTOMap.get(learningLesson.getCourseId());
            if(BeanUtils.isNotEmpty(courseSimpleInfoDTO)){
                learningPlanVO.setCourseName(courseSimpleInfoDTO.getName());
                learningPlanVO.setSections(courseSimpleInfoDTO.getSectionNum());
            }
            if(null != finalLessonCount.get(learningLesson.getId())){
                learningPlanVO.setWeekLearnedSections(Math.toIntExact(finalLessonCount.get(learningLesson.getId())));
            }
            return learningPlanVO;
        }).collect(Collectors.toList());

        LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
        learningPlanPageVO.setWeekTotalPlan(learningPlanVOList.stream()
                .mapToInt(vo -> vo.getWeekFreq() == null ? 0 : vo.getWeekFreq())
                .sum());
        learningPlanPageVO.setWeekFinished(learningPlanVOList.stream()
                .mapToInt(vo -> vo.getWeekLearnedSections() == null ? 0 : vo.getWeekLearnedSections())
                .sum());
        learningPlanPageVO.setWeekPoints(null);  //todo: 本周学习积分未赋值
        learningPlanPageVO.setTotal(page.getTotal());
        learningPlanPageVO.setPages(page.getPages());
        learningPlanPageVO.setList(learningPlanVOList);
        return learningPlanPageVO;
    }
}
