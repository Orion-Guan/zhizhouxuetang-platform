package com.tianji.promotion.annotation;


import com.tianji.promotion.enums.DistributedLockStrategyType;
import com.tianji.promotion.enums.DistributedLockType;

import javax.validation.constraints.NotNull;
import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 工作原理概述
 * <p>
 * 1. 注解的存在本身并不实现锁的逻辑，它仅充当“标记”。真正的分布式锁实现通常在切面、拦截器或注解处理器中读取这些属性，并依据 keyName、retryTime、expireTime 与
 * timeUnit 去获取、续期、释放锁（如使用 Redisson、Curator 等组件）。
 * 2. @Retention(RetentionPolicy.RUNTIME) 保证了注解在运行时仍然可用，方便框架通过反射读取属性值。
 * 3. @Target(ElementType.METHOD) 限定只能在方法上使用，适合对业务方法进行粒度控制，而不是整个类或字段。
 * 4. @Documented 让javadoc在生成时能够显示该注解的说明，提升可读性。
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface DistributedLock {

    // 锁类型
    DistributedLockType lockType() default DistributedLockType.REENTRANT_LOCK;

    // 所策略
    DistributedLockStrategyType lockStrategy() default DistributedLockStrategyType.FAIL_AFTER_RETRY_TIMEOUT;

    // 锁对象KEY
    String keyName();

    // 获取锁失败重试时长
    long retryTime() default 1L;

    // KEY锁的生存时间（-1L会触发看门狗机制不断给锁续命）
    long expireTime() default -1L;

    // 时间单位
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
