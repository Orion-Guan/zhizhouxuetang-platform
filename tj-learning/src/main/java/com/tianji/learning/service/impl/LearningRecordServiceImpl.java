package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-05
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService iLearningLessonService;


    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        //获取当前用户
        Long userId = UserContext.getUser();

        //获取指定课表信息
        LambdaQueryWrapper<LearningLesson> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId);
        LearningLesson learningLesson = iLearningLessonService.getBaseMapper().selectOne(wrapper);

        //查询其课表学习记录
        if(null == learningLesson){
            return null;
        }
        LambdaQueryWrapper<LearningRecord> recordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recordLambdaQueryWrapper.eq(LearningRecord::getLessonId, learningLesson.getId());
        List<LearningRecord> learningRecordList = this.list(recordLambdaQueryWrapper);

        //封装返回数据
        LearningLessonDTO learningLessonDTO = new LearningLessonDTO();
        learningLessonDTO.setId(learningLesson.getId());
        learningLessonDTO.setLatestSectionId(learningLesson.getLatestSectionId());
        learningLessonDTO.setRecords(BeanUtils.copyToList(learningRecordList, LearningRecordDTO.class));
        return learningLessonDTO;
    }
}
