package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 前端控制器
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
@Slf4j
@Api(tags = "积分记录相关接口")
public class PointsRecordController {

    private final IPointsRecordService iPointsRecordService;


    @GetMapping("today")
    @ApiOperation("查询用户已获得的今日积分统计")
    public List<PointsStatisticsVO> queryUserPointsOfDay(){
        return iPointsRecordService.queryUserPointsOfDay();
    }

}
