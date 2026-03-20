package com.tianji.learning.utils;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延迟任务类，用于管理延迟执行的任务
 */
@Data
public class DelayTaskForLearnRecord<D> implements Delayed {

    private  D  dataInfo;

    private long deadlineTime;

    /**
     * 创建一个延迟任务对象
     * @param dataInfo
     * @param duration
     */
    public DelayTaskForLearnRecord(D dataInfo, @NotNull Duration duration) {
        this.dataInfo = dataInfo;
        this.deadlineTime = System.nanoTime() + duration.toNanos();
    }

    /**
     * 获取延迟任务剩余的延迟时间
     *
     * @param unit 时间单位，用于指定返回延迟值的单位
     * @return 剩余的延迟时间，以指定的时间单位计算
     */
    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(Math.max(0,this.deadlineTime - System.nanoTime()),TimeUnit.NANOSECONDS);
    }

    /**
     * 比较当前延迟任务与另一个延迟对象的优先级
     *
     * @param o 要比较的延迟对象
     * @return 比较结果：负数表示当前对象优先级高（先执行），正数表示参数对象优先级高，0 表示优先级相同
     */
    public int compareTo(@NotNull Delayed o) {
        // 计算两个延迟任务的剩余时间差值
        long l = this.getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        // 根据差值返回比较结果：相等返回 0，当前任务延迟大返回 1，否则返回 -1
        return l == 0? 0: (l>0? 1: -1);
    }
}
