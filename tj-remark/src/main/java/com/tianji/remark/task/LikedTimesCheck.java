package com.tianji.remark.task;

import cn.hutool.core.collection.CollUtil;
import com.tianji.remark.service.ILikedRecordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
//@RefreshScope
@Data
@ConfigurationProperties(prefix = "hm.remark")
public class LikedTimesCheck {

    private List<String> bizTypes;

    private Long maxCheckCount;

    private final ILikedRecordService iLikedRecordService;

    /**
     * 执行时机：在上一次任务执行完成后，延迟30秒（30000毫秒）再次执行
     * 用途：定期同步点赞数量到数据库或其他存储
     * 特点：确保任务不会并发执行，前一次完成后再等待30秒启动下一次
     */
    @Scheduled(fixedDelay = 30000L)
    public void SyncTheNumberOfLikes(){
        //循环缓存中所有业务的点赞记录批量持久化
        if (CollUtil.isEmpty(bizTypes) || maxCheckCount == null) {
            log.info("nacos配置项中无要同步的业务(点赞数):{},{}",bizTypes,maxCheckCount);
            return;
        }

        bizTypes.forEach(bizType -> {
            iLikedRecordService.SyncTheNumberOfLikes(bizType,maxCheckCount);
        });
    }
}
