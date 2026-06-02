package com.tianji.promotion.enums;

import com.tianji.promotion.annotation.DistributedLock;
import com.tianji.promotion.service.ICouponService;
import org.redisson.api.RLock;

public enum DistributedLockStrategyType {
    // 快速跳过
    SKIP_FAST() {
        public boolean getLock(RLock rLock, DistributedLock distributedLock) throws InterruptedException {
            return rLock.tryLock(0, distributedLock.expireTime(), distributedLock.timeUnit());
        }
    },

    // 快速失败
    FAIL_FAST() {
        @Override
        public boolean getLock(RLock rLock, DistributedLock distributedLock) throws InterruptedException {
            boolean isSuccess = rLock.tryLock(0, distributedLock.expireTime(), distributedLock.timeUnit());
            if (!isSuccess) {
                throw new InterruptedException("请勿频繁操作，稍后重试！");
            }
            return true;
        }
    },

    // 无限重试
    KEEP_TRYING() {
        @Override
        public boolean getLock(RLock rLock, DistributedLock distributedLock) throws InterruptedException {
            rLock.lock(distributedLock.expireTime(), distributedLock.timeUnit());
            return true;
        }
    },

    // 重试超时后结束
    KIP_AFTER_RETRY_TIMEOUT() {
        @Override
        public boolean getLock(RLock rLock, DistributedLock distributedLock) throws InterruptedException {
            return rLock.tryLock(distributedLock.retryTime(), distributedLock.expireTime(), distributedLock.timeUnit());
        }
    },

    // 重试超时后抛出异常
    FAIL_AFTER_RETRY_TIMEOUT() {
        @Override
        public boolean getLock(RLock rLock, DistributedLock distributedLock) throws InterruptedException {
            boolean isSuccess = rLock.tryLock(distributedLock.retryTime(), distributedLock.expireTime(), distributedLock.timeUnit());
            if (!isSuccess) {
                throw new InterruptedException("请勿频繁操作，稍后重试！");
            }
            return true;
        }
    };

    /**
     * 枚举类抽象方法： 枚举类中的常数实例对象必须实现其枚举类中的抽象方法(每个枚举常数实例都可以定义自己的方法)
     */
    public abstract boolean getLock(RLock rLock, DistributedLock distributedLock) throws InterruptedException;
}