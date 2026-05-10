package com.tianji.learning.mq;


import cn.hutool.json.JSONUtil;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.message.SignedMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 监听积分记录：添加用户获得的积分记录
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class LearningPointsListener {

    private final IPointsRecordService iPointsRecordService;



    /**
     * 监听签到消息并处理积分记录
     * <p>
     * 该方法作为RabbitMQ消息监听器，监听签到相关的消息队列。
     * 当接收到用户签到消息时，会自动调用积分服务添加对应的积分记录。
     * </p>
     *
     * @param signedMessage 签到消息对象，包含用户签到的相关信息（如用户ID、签到时间等）
     */
    @RabbitListener(bindings = {
            @QueueBinding(
                    value = @Queue(name = "sign.points.queue", durable = "true", arguments = {@Argument(name = "x-queue-mode", value = "lazy")}),
                    exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
                    key = {MqConstants.Key.SIGN_IN}
            )
    })
    public void listenSignMessage(SignedMessage signedMessage){
        log.debug("监听到签到积分的消息通知:{}", JSONUtil.toJsonStr(signedMessage));
        iPointsRecordService.addPointsRecord(signedMessage.getUserId(), PointsRecordType.SIGN, signedMessage.getPoints());
    }


    /**
     * 监听问答消息并添加积分记录
     * @param userId
     */
    @RabbitListener(bindings = {
            @QueueBinding(
                   value = @Queue(name = "qa.points.queue", durable = "true", arguments = {@Argument(name = "x-queue-mode", value = "lazy")}),
                    exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
                    key = {MqConstants.Key.WRITE_REPLY}
            )
    })
    public void listenQAMessage(long userId){
        log.debug("监听到问答积分的消息通知:{}", JSONUtil.toJsonStr(userId));
        iPointsRecordService.addPointsRecord(userId, PointsRecordType.QA, 5);
    }



}
