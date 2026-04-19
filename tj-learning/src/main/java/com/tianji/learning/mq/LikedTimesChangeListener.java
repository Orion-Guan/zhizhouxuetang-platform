package com.tianji.learning.mq;


import cn.hutool.json.JSONUtil;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikedTimesChangeListener {

    private final IInteractionReplyService interactionReplyService;

    /**
     * 监听并处理问答系统的点赞次数变更消息
     * <p>
     * 该方法作为RabbitMQ消费者，监听点赞次数变更队列，当接收到点赞次数变化消息时，
     * 更新对应的互动回复记录中的点赞次数字段。
     * </p>
     *
     */
    @RabbitListener(bindings = {
            @QueueBinding(
                    value = @Queue(name = "liked.times.change.queue", durable = "true", arguments = {@Argument(name = "x-queue-mode", value = "lazy")}),
                    exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
                    key = {MqConstants.Key.QA_LIKED_TIMES_KEY}
            )
    })
    public void clickTimesChange(List<LikedTimesDTO> likedTimesDTOs) {
        log.debug("问答系统点赞次数改变消费者:{}", JSONUtil.toJsonStr(likedTimesDTOs));

        List<InteractionReply> interactionReplyList = likedTimesDTOs.stream().map(LikedTimesDTO -> {
            InteractionReply interactionReply = new InteractionReply();
            interactionReply.setId(LikedTimesDTO.getBizId())
                    .setLikedTimes(Math.toIntExact(LikedTimesDTO.getLikedTimes()));
            return interactionReply;
        }).collect(Collectors.toList());

        interactionReplyService.updateBatchById(interactionReplyList);
    }
}
