package com.tianji.promotion.factory;

import com.tianji.promotion.enums.DistributedLockType;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 简单工厂模式: 获取分布式锁对象
 */
@Component
public class DistributedLockFactory {
    // 如果map的key是枚举类型推荐使用EnumMap，性能更好些。
    private final Map<DistributedLockType, Function<String, RLock>> lockTypeFuncMap = new EnumMap<>(DistributedLockType.class);

    public DistributedLockFactory(RedissonClient redissonClient) {
        lockTypeFuncMap.put(DistributedLockType.REENTRANT_LOCK, redissonClient::getLock);
        lockTypeFuncMap.put(DistributedLockType.FAIR_LOCK, redissonClient::getFairLock);
        lockTypeFuncMap.put(DistributedLockType.READ_LOCK, name -> redissonClient.getReadWriteLock(name).readLock());
        lockTypeFuncMap.put(DistributedLockType.WRITE_LOCK, name -> redissonClient.getReadWriteLock(name).writeLock());
    }


    /**
     * 获取锁对象
     */
    public RLock getLock(String lockName, DistributedLockType distributedLockType) {
        return lockTypeFuncMap.get(distributedLockType).apply(lockName);
    }
}
