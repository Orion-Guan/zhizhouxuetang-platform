package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-05
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId
     * @return
     */
    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    /**
     * 添加学习记录
     * @param learningRecordFormDTO
     */
    void addLearningRecord(LearningRecordFormDTO learningRecordFormDTO);

    /**
     * 根据课表Id集合查询其下所有课程章节的学习记录
     * @param lessonIdSet
     * @return
     */
    List<LearningRecord> queryLearningRecordByLessonIds(Set<Long> lessonIdSet);
}
