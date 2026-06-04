package com.tianji.promotion.handler.mq;

import com.rabbitmq.client.AMQP;
import com.tianji.common.constants.MqConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.service.impl.UserCouponServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiveCouponConsumer {

    private final UserCouponServiceImpl userCouponService;

    @RabbitListener(
            bindings = {
                    @QueueBinding(
                            value = @Queue(name = MqConstants.Queue.USER_COUPON_QUEUE, durable = "true", arguments = {@Argument(name = "x-queue-mode", value = "lazy")}),
                            exchange = @Exchange(name = MqConstants.Exchange.USER_COUPON_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
                            key = MqConstants.Key.USER_COUPON_KEY
                    )
                }
    )
    public void userCouponListener(UserCouponDTO userCouponDTO) {
        log.info("领取优惠券消费者接收到消息: {}", userCouponDTO);
        userCouponService.checkLimitAndSaveUserCoupon(userCouponDTO);
        log.info("领取优惠券消费者处理完成");
    }

}
