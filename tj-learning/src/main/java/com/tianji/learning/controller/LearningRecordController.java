package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

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
}
