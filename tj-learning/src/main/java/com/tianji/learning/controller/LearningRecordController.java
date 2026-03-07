package com.tianji.learning.controller;


import cn.hutool.json.JSONUtil;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-05
 */
@RestController
@RequestMapping("learning-records")
@RequiredArgsConstructor
@Api(tags = "学习记录相关接口")
@Slf4j
public class LearningRecordController {

    private final ILearningRecordService iLearningRecordService;

    /**
     * 查询当前用户指定课程的学习进度
     *
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @GetMapping("/course/{courseId}")
    @ApiOperation("查询当前用户指定课程的学习记录")
    LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId) {
        log.debug("查询当前用户指定课程的学习记录入参: {}",courseId);
        return iLearningRecordService.queryLearningRecordByCourse(courseId);
    }

    /**
     * 添加学习记录
     * @param learningRecordFormDTO
     */
    @PostMapping
    @ApiOperation("添加学习记录")
    public void addLearningRecord(@RequestBody LearningRecordFormDTO learningRecordFormDTO){
        log.debug("添加学习记录请求入参：{}", JSONUtil.toJsonStr(learningRecordFormDTO));
        iLearningRecordService.addLearningRecord(learningRecordFormDTO);
    }
}
