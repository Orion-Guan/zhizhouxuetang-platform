package com.tianji.learning.controller;


import cn.hutool.json.JSONUtil;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-20
 */
@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
@Slf4j
@Api(tags = "问题回答或评论相关接口")
public class InteractionReplyController {

    private final IInteractionReplyService interactionReplyService;

    /**
     * 新增问题的回答或评论
     * @param replyDTO
     */
    @PostMapping
    @ApiOperation("新增问题的回答或评论")
    public void saveAC(@RequestBody ReplyDTO replyDTO){
        log.debug("新增问题的回答或评论入参: {}", JSONUtil.toJsonStr(replyDTO));
        interactionReplyService.saveAC(replyDTO);
    }
}
