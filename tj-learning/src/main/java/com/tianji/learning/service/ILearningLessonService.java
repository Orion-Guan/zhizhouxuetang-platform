package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-01-18
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    void addLessons(Long orderId, Long userId, List<Long> courseIds);

    /**
     * 获取我的课程
     * @return
     */
    PageDTO<LearningLessonVO> getMyLessons(PageQuery pageQuery);


    /**
     * 获取用户最近学习课程
     * @return
     */
    LearningLessonVO getRecentLearning();


    /**
     * MQ用户退款删除用户课程
     * @param orderBasicDTO
     */
    void removeLessonOfRefund(OrderBasicDTO orderBasicDTO);

    /**
     * MQ课程删除用户课程
     * @param courseId
     */
    void removeLessonById(String courseId);

    /**
     * 校验当前用户是否可以学习当前课程
     * @param courseId
     * @return
     */
    Long checkCourseValid(Long courseId);


    /**
     * 获取用户课程状态
     * @param courseId
     * @return
     */
    LearningLessonVO getLessonStatus(Long courseId);


    /**
     * 获取课程学习人数
     * @param courseId
     *
     * @return
     */
    Integer getLearningCountById(Long courseId);


    void updateLearningInfo(LearningRecordFormDTO learningRecordFormDTO, LearningLesson learningLesson);

    /**
     * 创建学习计划
     * @param learningPlanDTO
     */
    void createLearningPlan(LearningPlanDTO learningPlanDTO);

    /**
     * 查询用户学习计划信息
     * @param pageQuery
     * @return
     */
    LearningPlanPageVO getCoursePlans(PageQuery pageQuery);
}
