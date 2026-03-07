package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final CourseClient courseClient;


    /**
     * 查询当前用户指定课程的学习进度
     *
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
        if (null == learningLesson) {
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

    
    /**
     * 添加学习记录
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void addLearningRecord(LearningRecordFormDTO learningRecordFormDTO) {
        //更新课表状态
        LearningLesson learningLesson = iLearningLessonService.getById(learningRecordFormDTO.getLessonId());
        assert learningLesson != null;
        if(learningLesson.getStatus() == LessonStatus.NOT_BEGIN){
            LambdaUpdateWrapper<LearningLesson> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(LearningLesson::getStatus, LessonStatus.LEARNING)
                    .eq(LearningLesson::getId,learningRecordFormDTO.getLessonId());
            boolean update = iLearningLessonService.update(wrapper);
            if (!update) {
                throw new DbException("更新课表学习状态失败!");
            }
        }

        //判断考试还是视频
        LearningRecord learningRecord = null;
        if (SectionType.EXAM == learningRecordFormDTO.getSectionType()) {
            examProcess(learningRecordFormDTO, learningLesson);
            return;
        }

        //判断是否首次学习该视频
        LambdaQueryWrapper<LearningRecord> learningRecordWrapper = new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getLessonId, learningRecordFormDTO.getLessonId())
                .eq(LearningRecord::getSectionId, learningRecordFormDTO.getSectionId())
                .eq(LearningRecord::getUserId,UserContext.getUser());
        LearningRecord learningRecord1 = this.getOne(learningRecordWrapper);
        if(learningRecord1 == null){
            firestLearning(learningRecordFormDTO, learningLesson);
            return;
        }

        //判断是否已学完
        boolean isFinished = learningRecordFormDTO.getMoment() * 2 >= learningRecordFormDTO.getDuration();
        if(!isFinished){
            notFinished(learningRecordFormDTO, learningRecord1);
            return;
        }

        //判断是否首次学完
        if(!learningRecord1.getFinished()){
            firstLearned(learningRecordFormDTO, learningRecord1, learningLesson);
            return;
        }

        //更新学习记录信息
        notFirstFinished(learningRecordFormDTO, learningRecord1);
    }



    private void notFirstFinished(LearningRecordFormDTO learningRecordFormDTO, LearningRecord learningRecord1) {
        LearningRecord learningRecord4 = new LearningRecord()
                .setId(learningRecord1.getId())
                .setMoment(learningRecordFormDTO.getMoment())
                .setFinished(true)
                .setFinishTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        this.updateById(learningRecord4);

        //更新课表信息
        LearningLesson learningLesson1 = new LearningLesson()
                .setId(learningRecordFormDTO.getLessonId())
                .setLatestSectionId(learningRecordFormDTO.getSectionId())
                .setLatestLearnTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        iLearningLessonService.updateById(learningLesson1);
    }


    private void examProcess(LearningRecordFormDTO learningRecordFormDTO, LearningLesson learningLesson) {
        LearningRecord learningRecord;
        //新增学习记录
        learningRecord = BeanUtils.copyBean(learningRecordFormDTO, LearningRecord.class);
        learningRecord.setUserId(UserContext.getUser())
                .setFinished(true)
                .setCreateTime(learningRecordFormDTO.getCommitTime())
                .setFinishTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        boolean save = this.save(learningRecord);
        if(!save){
            throw new DbException("保存学习记录失败");
        }

        //更新课表信息
        iLearningLessonService.updateLearningInfo(learningRecordFormDTO, learningLesson);
    }

    private void firestLearning(LearningRecordFormDTO learningRecordFormDTO, LearningLesson learningLesson) {
        LearningRecord learningRecord;
        learningRecord = BeanUtils.copyBean(learningRecordFormDTO, LearningRecord.class);
        learningRecord.setUserId(UserContext.getUser())
                .setMoment(learningRecordFormDTO.getMoment())
                .setFinished(false)
                .setCreateTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        boolean save = this.save(learningRecord);
        if(!save){
            throw new DbException("保存学习记录失败");
        }
        learningLesson.setLatestSectionId(learningRecordFormDTO.getSectionId())
                .setLatestLearnTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        boolean update1 = iLearningLessonService.updateById(learningLesson);
        if(!update1){
            throw new DbException("保存学习记录失败");
        }
    }

    private void notFinished(LearningRecordFormDTO learningRecordFormDTO, LearningRecord learningRecord1) {
        //更新学习记录信息
        LearningRecord learningRecord2 = new LearningRecord()
                .setId(learningRecord1.getId())
                .setMoment(learningRecordFormDTO.getMoment())
                .setFinished(false)
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        this.updateById(learningRecord2);

        //更新课表信息
        LearningLesson learningLesson1 = new LearningLesson()
                .setId(learningRecordFormDTO.getLessonId())
                .setLatestSectionId(learningRecordFormDTO.getSectionId())
                .setLatestLearnTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        iLearningLessonService.updateById(learningLesson1);
    }

    private void firstLearned(LearningRecordFormDTO learningRecordFormDTO, LearningRecord learningRecord1, LearningLesson learningLesson) {
        //更新学习记录信息
        LearningRecord learningRecord3 = new LearningRecord()
                .setId(learningRecord1.getId())
                .setMoment(learningRecordFormDTO.getMoment())
                .setFinished(true)
                .setFinishTime(learningRecordFormDTO.getCommitTime())
                .setUpdateTime(learningRecordFormDTO.getCommitTime());
        this.updateById(learningRecord3);

        //更新课表信息
        iLearningLessonService.updateLearningInfo(learningRecordFormDTO, learningLesson);
        return;
    }
}
