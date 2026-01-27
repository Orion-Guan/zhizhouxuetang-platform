package com.tianji.learning.mq;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class LessonChangeListener {

    private final ILearningLessonService lessonService;

    /**
     * 课程购买成功后需要将课程加入课表监听器
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "learning.lesson.pay.queue", durable = "true", arguments = @Argument(name = "x-queue-mode", value = "lazy")),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
            key = {MqConstants.Key.ORDER_PAY_KEY}
    ))
    public void listenCoursePay(OrderBasicDTO orderBasicDTO){
        //健壮处理
        if (orderBasicDTO == null || orderBasicDTO.getUserId() == null || CollUtils.isEmpty(orderBasicDTO.getCourseIds())){
            log.error("接收MQ消息有误，订单数据为空：{}", JSONUtil.toJsonStr(orderBasicDTO));
            return;
        }
        //避免网络故障导致消费者处理完消息后返回的ACK未被其队列收到，网络恢复后队列重复投递消息的问题。
        LambdaQueryWrapper<LearningLesson> queryWrapper = new LambdaQueryWrapper<LearningLesson>()
                .eq(LearningLesson::getUserId, orderBasicDTO.getUserId())
                .in(LearningLesson::getCourseId, orderBasicDTO.getCourseIds());
        List<LearningLesson> lessonList = lessonService.getBaseMapper().selectList(queryWrapper);
        if(CollUtils.isNotEmpty(lessonList)){
            log.debug("用户{}，课程{}已存在，无需添加重复消费", orderBasicDTO.getUserId(), orderBasicDTO.getCourseIds());
            return;
        }

        //添加课程表
        log.debug("订单ID{},用户{},购买课程{}，开始添加课程表",orderBasicDTO.getOrderId(), orderBasicDTO.getUserId(), orderBasicDTO.getCourseIds());
        lessonService.addLessons(orderBasicDTO.getOrderId(),orderBasicDTO.getUserId(),orderBasicDTO.getCourseIds());
    }


    /**
     * 用户退款，删除用户购买的此课程
     * @param orderBasicDTO
     */
    @RabbitListener(bindings = {
            @QueueBinding(
                    value = @Queue(name = "learning.lesson.refund.queue", durable = "true", arguments = {@Argument(name = "x-queue-mode", value = "lazy")}),
                    exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC, durable = "true"),
                    key = {MqConstants.Key.ORDER_REFUND_KEY}
            )
    })
    public void listenCourseWithDrow(OrderBasicDTO orderBasicDTO){
        //健壮处理
        if(null == orderBasicDTO || null == orderBasicDTO.getUserId() || CollUtils.isEmpty(orderBasicDTO.getCourseIds())){
            log.error("用户退款删除课表失败:{}", JSONUtil.toJsonStr(orderBasicDTO));
            return;
        }

        //删除用户课表
        lessonService.removeLessonOfRefund(orderBasicDTO);
    }
}
