package com.tianji.learning.controller;


import cn.hutool.json.JSONUtil;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * @param pageQuery
     * @return
     */
    @GetMapping("page")
    @ApiOperation("我的课程分页查询")
    public PageDTO<LearningLessonVO> getMyLessons(PageQuery pageQuery) {
        log.info("我的课程分页查询入参:{}", JSONUtil.toJsonStr(pageQuery));
        return iLearningLessonService.getMyLessons(pageQuery);
    }
}
