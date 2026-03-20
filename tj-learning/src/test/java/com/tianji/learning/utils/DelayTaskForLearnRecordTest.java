package com.tianji.learning.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.DelayQueue;

@Slf4j
class DelayTaskForLearnRecordTest {


    /**
     * 测试自定义的延时定时任务
     * @throws InterruptedException
     */
    @Test
    public void delayQueue() throws InterruptedException {
        DelayQueue<DelayTaskForLearnRecord<String>> queue = new DelayQueue<>();
        log.info("延迟队列开始执行。。。。");
        DelayTaskForLearnRecord<String> learnRecord = new DelayTaskForLearnRecord<>("学习记录3", Duration.ofSeconds(30));
        DelayTaskForLearnRecord<String> learnRecord2 = new DelayTaskForLearnRecord<>("学习记录2", Duration.ofSeconds(20));
        DelayTaskForLearnRecord<String> learnRecord1 = new DelayTaskForLearnRecord<>("学习记录1", Duration.ofSeconds(1));
        queue.add(learnRecord);
        queue.add(learnRecord2);
        queue.add(learnRecord1);
        while (true){
            DelayTaskForLearnRecord<String> take = queue.take();  //阻塞等待获取需要执行的任务（超时任务）
            log.info("take = " + take);
        }
    }
}