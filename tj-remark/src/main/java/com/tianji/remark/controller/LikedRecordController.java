package com.tianji.remark.controller;


import cn.hutool.json.JSONUtil;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-07
 */
@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Slf4j
@Api(tags = "点赞服务相关接口")
public class LikedRecordController {

    private final ILikedRecordService likedRecordService;

    /**
     * 添加或取消点赞记录
     * @param likeRecordFormDTO
     */
    @PostMapping
    @ApiOperation("增减点赞数")
    public void increaseOrDecreaseLikes(@RequestBody @Valid LikeRecordFormDTO likeRecordFormDTO) {
        log.debug("增减点赞入参:{}", JSONUtil.toJsonStr(likeRecordFormDTO));
        likedRecordService.addLikeRecord(likeRecordFormDTO);
    }


    /**
     * 查询用户指定业务点赞状态
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询用户指定业务点赞状态")
    public Set<Long> getCLickStatusByBiz(@RequestParam("biz") List<Long> biz){
        log.info("查询用户指定业务点赞状态:{}",biz);
        return likedRecordService.getCLickStatusByBiz(biz);
    }
}
