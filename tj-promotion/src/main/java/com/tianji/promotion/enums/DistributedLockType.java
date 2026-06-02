package com.tianji.promotion.enums;

/**
 * 分布式锁类型
 */
public enum DistributedLockType {

    // 可重入锁
    REENTRANT_LOCK,

    // 公平锁
    FAIR_LOCK,

    // 读锁
    READ_LOCK,

    // 写锁
    WRITE_LOCK;
}
