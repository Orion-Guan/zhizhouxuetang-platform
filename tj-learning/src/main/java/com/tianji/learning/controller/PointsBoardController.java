package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@Api(tags = "学霸天梯榜相关接口")
public class PointsBoardController {

    private final IPointsBoardService iPointsBoardService;


    @GetMapping
    @ApiOperation("查询积分榜单")
    public PointsBoardVO pageList(PointsBoardQuery pointsBoardQuery){
        return iPointsBoardService.queryPointsBoards(pointsBoardQuery);
    }
}
