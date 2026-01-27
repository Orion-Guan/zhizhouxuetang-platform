package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSearchDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    /**
     * 添加用户购买的学习课程到课程表
     * @param userId
     * @param courseIds
     */
    @Override
    @Transient
    public void addLessons(Long orderId,Long userId, List<Long> courseIds) {
        //获取课程过期时间
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(simpleInfoList)){
            log.error("MQ消费者为{}订单{}用户添加课程{}失败：系统中无此课程ID",orderId, userId, courseIds);
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
     * @return
     */
    @Override
    public PageDTO<LearningLessonVO> getMyLessons(PageQuery pageQuery) {
        //获取当前登录用户
        Long userId = UserContext.getUser();
        if(userId == null){
            log.error("用户未登录");
            throw new BadRequestException("请先登录系统");
        }

        //获取用户课程表
        Page<LearningLesson> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        if(StringUtils.isEmpty(pageQuery.getSortBy())){
            page.addOrder(OrderItem.desc("latest_learn_time"));
        }else {
            page.addOrder(new OrderItem(pageQuery.getSortBy(), pageQuery.getIsAsc()));
        }
        Page<LearningLesson> paged = this.lambdaQuery().eq(LearningLesson::getUserId, userId).page(page);

        //封装VO
        List<LearningLesson> records = paged.getRecords();
        if(CollUtils.isEmpty(records)){
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

        return PageDTO.of(paged,learningLessonVOS);
    }

    /**
     * 获取用户最近学习课程
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
        if(null == learningLesson){
            log.error("用户{}:无最近正在学习的课程",userId);
            return null;
        }
        LearningLessonVO learningLessonVO = BeanUtils.copyBean(learningLesson, LearningLessonVO.class);

        //远程调用获取课程相关信息
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(Collections.singleton(learningLesson.getLatestSectionId()));
        if(!CollUtils.isEmpty(cataSimpleInfoDTOS)){
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
}
