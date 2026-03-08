package com.tianji.learning.controller;


import cn.hutool.json.JSONUtil;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-01-18
 */
@RestController
@RequestMapping("/lessons")
@Api(tags = "我的课程表相关接口")
@RequiredArgsConstructor
@Slf4j
public class LearningLessonController {

    private final ILearningLessonService iLearningLessonService;

    /**
     * 我的课程分页查询
     *
     * @param pageQuery
     * @return
     */
    @GetMapping("page")
    @ApiOperation("我的课程分页查询")
    public PageDTO<LearningLessonVO> getMyLessons(PageQuery pageQuery) {
        log.info("我的课程分页查询入参:{}", JSONUtil.toJsonStr(pageQuery));
        return iLearningLessonService.getMyLessons(pageQuery);
    }

    /**
     * 获取用户最近学习课程
     *
     * @return
     */
    @GetMapping("now")
    @ApiOperation("用户最近学习课程")
    public LearningLessonVO getRecentLearning() {
        return iLearningLessonService.getRecentLearning();
    }


    /**
     * 删除用户某课程
     *
     * @param courseId
     */
    @DeleteMapping("{courseId}")
    @ApiOperation("删除用户某课程")
    public void removeLessonById(@PathVariable String courseId) {
        log.info("删除课程入参:{}", courseId);
        iLearningLessonService.removeLessonById(courseId);
    }

    /**
     * 校验课程是否可学习
     *
     * @param courseId
     * @return
     */
    @GetMapping("/{courseId}/valid")
    @ApiOperation("效验课程用户是否可学习")
    public Long checkCourseValid(@PathVariable("courseId") Long courseId) {
        log.debug("效验课程:{}", courseId);
        return iLearningLessonService.checkCourseValid(courseId);
    }


    /**
     * 查询用户已报名课程状态
     *
     * @param courseId
     * @return
     */
    @GetMapping("/{courseId}")
    @ApiOperation("查询用户已报名课程状态")
    public LearningLessonVO getLessonStatus(@PathVariable("courseId") Long courseId) {
        log.debug("查询用户{}已报名课程状态入参:{}", UserContext.getUser(), courseId);
        return iLearningLessonService.getLessonStatus(courseId);
    }


    /**
     * 统计课程学习人数
     *
     * @param courseId 课程id
     * @return 学习人数
     */
    @GetMapping("/{courseId}/count")
    @ApiOperation("统计课程学习人数")
    public Integer countLearningLessonByCourse(@NotNull(message = "课程ID不能为空") @PathVariable("courseId") Long courseId) {
        log.debug("统计课程学习人数入参courseId:{}", courseId);
        return iLearningLessonService.getLearningCountById(courseId);
    }

    /**
     * 添加用户指定课程学习计划
     *
     * @param learningPlanDTO
     */
    @PostMapping("plans")
    @ApiOperation("添加用户指定课程学习计划")
    public void createLearningPlan(@Valid @RequestBody LearningPlanDTO learningPlanDTO) {
        log.info("添加用户指定课程学习计划入参:{}",JSONUtil.toJsonStr(learningPlanDTO));
        iLearningLessonService.createLearningPlan(learningPlanDTO);
    }


    /**
     * 查询用户学习计划信息（分页）
     *
     * @param pageQuery 分页查询参数，包含页码、 pageSize、排序等信息
     * @return LearningPlanPageVO 用户学习计划分页数据
     */
    @GetMapping("plans")
    @ApiOperation("查询用户学习计划信息")
    public LearningPlanPageVO getCoursePlans(@Valid PageQuery pageQuery){
        log.debug("查询用户学习计划信息入参：{}",JSONUtil.toJsonStr(pageQuery));
        return iLearningLessonService.getCoursePlans(pageQuery);
    }
}
