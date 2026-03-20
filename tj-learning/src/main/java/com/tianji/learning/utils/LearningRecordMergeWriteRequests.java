package com.tianji.learning.utils;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;



@Slf4j
@RequiredArgsConstructor
@Component
public class LearningRecordMergeWriteRequests {

    private final StringRedisTemplate redisTemplate;

    private final String REDIS_KEY_FORMAT = "learning:record:{}";

    private final ILearningLessonService lessonService;

    private final LearningRecordMapper learningRecordMapper;

    /**
     *  static: 类加载时被加载，在内存中只保留一个副本
     *  volatile: 可变的变量。即在多线程中对该变量的修改其他线程可见（可见性、有序性）。
     */
    private static volatile boolean running = true;

    private final DelayQueue<DelayTaskForLearnRecord<DelayTaskDateType>> queue = new DelayQueue<>();


    /**
     * 项目启动--->容器被初始化-->在 Bean 创建完成后自动执行(即该Bean的构造方法被执行完之后)
     * 启动异步线程处理延迟队列中的学习记录持久化任务
     */
    @PostConstruct
    public void init(){
        log.trace("学习记录异步延时任务开始执行同步数据......");
        CompletableFuture.runAsync(this::handleDelayTask);
    }

    /**
     * Bean 销毁前回调方法
     * 停止异步处理线程，确保延时任务线程安全退出
     */
    @PreDestroy
    public void BeanDestroyBefore(){
        running = false;
        log.trace("学习记录异步延时任务执行停止......");
    }

    public void handleDelayTask(){
        try {
            while (running){
                //获取到期的延时任务（会阻塞等待，休眠）
                DelayTaskForLearnRecord<DelayTaskDateType> take = queue.take();
                DelayTaskDateType dataInfo = take.getDataInfo();
                //比较缓存中的数据：如果不一致-->说明用户还在观看此小结视频，如果一致--->则说明用户已经不在观看此小结视频(因为：前端会每间隔15秒提交一次学习记录，此处是20S执行一次同步，在此之前如果用户继续观看视频、缓存中的此小结学习进度值肯定会变化，反之不会)
                LearningRecord learningRecord = this.readCacheDb(dataInfo.lessonId, dataInfo.sectionId);
                if(learningRecord == null){
                    log.debug("未获取到缓存中的数据:{}",dataInfo);
                    continue;
                }

                //不一致
                if(!learningRecord.getMoment().equals(dataInfo.getMoment())){
                    log.debug("用户还在继续看视频，无需同步缓存数据到DB:{}",dataInfo);
                    continue;
                }

                //一致
                //修改学习记录表中该章节的学习进度
                learningRecord.setFinished(null);
                learningRecordMapper.updateById(learningRecord);
                //更新课表中最近学习章节和其最近学习时间
                LearningLesson learningLesson = new LearningLesson()
                        .setId(dataInfo.getLessonId())
                        .setLatestSectionId(dataInfo.getSectionId())
                        .setLatestLearnTime(LocalDateTime.now().minusSeconds(20));
                lessonService.updateById(learningLesson);
            }
        } catch (Exception e) {
           log.error("延时任务执行失败：",e);
        }
    }


    /**
     * 更新用户学习记录到Redis缓存
     * @param learningRecord
     */
    public void addLearningRecordTask(LearningRecord learningRecord){
        //添加学习记录到缓存
        writeLearningRecordToRedis(learningRecord);
        //将用户每次提交到缓存中的学习记录持久化到数据库（只持久最后一次提交的学习进度）
        DataPersistenceToDbTask(learningRecord);
    }

    /**
     * 读取缓存中的学习记录
     * @param lessonId
     * @param sectionId
     * @return
     */
    public LearningRecord readCacheDb(Long lessonId, Long sectionId){
        try {
            //读取缓存数据
            Object object = redisTemplate.opsForHash().get(StrUtil.format(REDIS_KEY_FORMAT, lessonId), sectionId.toString());
            if(null == object){
                return null;
            }
            return JSONUtil.toBean(object.toString(), LearningRecord.class);
        } catch (Exception e) {
            log.error("读取缓存数据失败：",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除缓存中的数据
     * @param lessonId
     * @param sectionId
     */
    public void deleteCacheDb(Long lessonId, Long sectionId){
        redisTemplate.opsForHash().delete(StrUtil.format(REDIS_KEY_FORMAT,lessonId),sectionId.toString());
    }


    public void writeLearningRecordToRedis(LearningRecord learningRecord) {
        //构建缓存数据结构
        String string = new LearningRecordCacheDb(learningRecord).toJson();
        String key = StringUtils.format(REDIS_KEY_FORMAT, learningRecord.getLessonId());
        //更新缓存学习进度
        redisTemplate.opsForHash().put(key,learningRecord.getSectionId().toString(),string);
        //设置过期时间
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    private void DataPersistenceToDbTask(LearningRecord learningRecord) {
        //封装延迟任务存放的数据
        DelayTaskDateType delayTaskDateType = new DelayTaskDateType(learningRecord);
        DelayTaskForLearnRecord<DelayTaskDateType> delayTaskForLearnRecord = new DelayTaskForLearnRecord<>(delayTaskDateType, Duration.ofSeconds(20));
        //将延时任务添加到延时队列中
        queue.add(delayTaskForLearnRecord);
    }


    //Redis缓存学习记录内部类
    @Data
    @NoArgsConstructor
    public static class LearningRecordCacheDb{
        private Long id;
        private Integer moment;
        private Boolean finished;

        public LearningRecordCacheDb(LearningRecord record) {
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }

        public String toJson(){
            return JSONUtil.toJsonStr(this);
        }
    }

    //延迟任务内部储存的数据内部类
    @Data
    @NoArgsConstructor
    private static class DelayTaskDateType{
        private Long lessonId;
        private Long sectionId;
        private Integer moment;

        public DelayTaskDateType(LearningRecord learningRecord) {
            this.lessonId = learningRecord.getLessonId();
            this.sectionId = learningRecord.getSectionId();
            this.moment = learningRecord.getMoment();
        }
    }
}
