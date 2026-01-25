package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;

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
}
