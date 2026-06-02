package com.tianji.promotion.aspect;

import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.promotion.factory.DistributedLockFactory;
import com.tianji.promotion.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MyLockAspect {

    private final RedissonClient redissonClient;

    private final DistributedLockFactory distributedLockFactory;

    /**
     * 1、系统获取@annotation注解括号中指定的变量名或注解全限定类名
     * 2、判断获取到的是变量名还是注解全限定类名。
     * 2.1 如果是全限定类名则直接拦截此注解标记的所有方法
     * 2.2 若是变量名则会从通知方法参数列表中找到同名的形参和其形参所属的类型，然后根据形参所属的类型推断出其要拦截的注解方法
     * 3、当注解标记的方法被执行时，AspectJ 会将目标执行方法的注解实例传递到此方法对应的形参中即distributedLock。
     */
    @Around("@annotation(distributedLock)")
    public Object addLocK(ProceedingJoinPoint proceedingJoinPoint, DistributedLock distributedLock) throws Throwable {
        RLock rLock = distributedLockFactory.getLock(distributedLock.keyName(), distributedLock.lockType());
        boolean isSuccess = distributedLock.lockStrategy().getLock(rLock, distributedLock);
        if(!isSuccess){
            // 获取锁失败直接跳过结束
            log.error("获取锁失败，直接结束");
            return null;
        }
        //执行目标业务方法
        try {
            return proceedingJoinPoint.proceed(); //返回目标方法的执行结果到调用方
        } finally {
            // 释放锁
            rLock.unlock();  // 底层会使用lua脚本（原子性）判断释放锁的线程与获取到锁的线程是否同一个，不是则不会释放。
        }
    }
}
