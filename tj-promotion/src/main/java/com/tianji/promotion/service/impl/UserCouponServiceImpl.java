package com.tianji.promotion.service.impl;

import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-05-24
 */
@Service
@Slf4j
@RequiredArgsConstructor
@EnableAspectJAutoProxy(exposeProxy = true) // 开启AOP自动代理并暴漏其代理对象
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;

    private final IExchangeCodeService exchangeCodeService;

    private final RedissonClient redissonClient;

    @Override
    public void receiveCoupon(Long couponId) throws InterruptedException {
        // 查询优惠券
        Coupon coupon = couponMapper.selectById(couponId);
        if (null == coupon) {
            log.error("优惠券不存在：{}", couponId);
            throw new BadRequestException("优惠券不存在");
        }
        //效验发放领取时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
            log.error("不在领取时间范围内");
            throw new BizIllegalException("不在领取时间范围内");
        }

        /*
            //效验库存（乐观锁特定场景下无需判断库存超卖问题：直接在更新库存数据前检查库存数是否小于总库存即可）
            if (coupon.getIssueNum() >= coupon.getTotalNum()) {
                log.error("优惠券库存不足：{}", couponId);
                throw new BizIllegalException("优惠券库存不足");
            }
        */

        /*
            //效验领取限额并保存数据(悲观锁-同步代码块：解决同个用户并发下领取优惠券超限问题---在服务多实例部署下仍然有并发带来的安全问题)
            synchronized (UserContext.getUser().toString().intern()) {
                // 通过获取UserCouponServiceImpl代理对象，来调用其事务方法
                UserCouponServiceImpl userCouponServiceAgency = (UserCouponServiceImpl) AopContext.currentProxy();
                userCouponServiceAgency.checkLimitAndSaveUserCoupon(coupon, now);
            }
        */

        // 获取锁对象: 防止单用户多次并发领取优惠券导致超限问题
        String key = "lock:promotion:uid:" + UserContext.getUser();
        RLock lock = redissonClient.getLock(key);
        boolean success = lock.tryLock(3, 30, TimeUnit.SECONDS);  //3：表示未获取到锁则排队3秒重试获取锁（底层会触发看门狗守护线程-重置key超时时间，每30/3=10秒）
        if (!success) {
            log.error("{}获取锁失败：{}", key, Thread.currentThread().getName());
            throw new BizIllegalException("勿频繁领取优惠券，请稍后重试");
        }
        try {
            // 通过获取UserCouponServiceImpl代理对象，来调用其事务方法
            UserCouponServiceImpl userCouponServiceAgency = (UserCouponServiceImpl) AopContext.currentProxy();
            userCouponServiceAgency.checkLimitAndSaveUserCoupon(coupon, now);
        } finally {
            // 不管执行成功与否，最后都要释放锁（底层会使用redis的Lua脚本：确保删除的key是本线程获取到的锁而不是别的线程获取到的锁）
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkLimitAndSaveUserCoupon(Coupon coupon, LocalDateTime now) {
        Long count = this.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .eq(UserCoupon::getCouponId, coupon.getId())
                .count();
        if (null != count && count >= coupon.getUserLimit()) {
            log.error("用户领取次数已用尽：{}", coupon.getId());
            throw new BizIllegalException("用户领取次数已用尽");
        }

        //自增优惠券库存
        int rows = couponMapper.increaseIssueNum(coupon.getId());
        if (rows < 1) {
            log.error("优惠券库存不足：{}", coupon.getId());
            throw new BizIllegalException("优惠券库存不足");
        }

        //新增数据
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (null == termBeginTime || null == termEndTime) {
            termBeginTime = now;
            termEndTime = now.plusDays(coupon.getTermDays());
        }
        UserCoupon userCoupon = new UserCoupon()
                .setUserId(UserContext.getUser())
                .setCouponId(coupon.getId())
                .setStatus(UserCouponStatus.UNUSED)
                .setCreateTime(LocalDateTime.now())
                .setTermBeginTime(termBeginTime)
                .setTermEndTime(termEndTime);
        this.save(userCoupon);

        // throw new RuntimeException("测试事务--没有数据则事务生效");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangeCodeObtainCoupon(String code) {
        // 解析效验兑换码并获取里面的序列号（防篡改）
        long serialNo = CodeUtil.parseCode(code);

        //效验兑换码是否存在
        ExchangeCode exchangeCode = exchangeCodeService.getById(serialNo);
        if (null == exchangeCode) {
            log.error("兑换码不存在");
            throw new BizIllegalException("兑换码不存在");
        }

        // Redis设置码已使用并返回旧值
        Boolean isUsed = exchangeCodeService.setAndGetStatusForExchangeCode(serialNo, true);
        if (null != isUsed && isUsed) {
            log.error("兑换码已使用");
            throw new BizIllegalException("兑换码已使用");
        }

        try {
            // 检查兑换码是否过期
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(exchangeCode.getExpiredTime())) {
                log.error("兑换码已失效");
                throw new BizIllegalException("兑换码已失效");
            }
            //效验优惠券领取限额并保存用户领取优惠券的数据
            Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
            checkLimitAndSaveUserCoupon(coupon, now);
            //自增优惠券库存
            couponMapper.increaseIssueNum(coupon.getId());
            //更新兑换码状态
            ExchangeCode exchangeCode1 = new ExchangeCode()
                    .setId(exchangeCode.getId())
                    .setStatus(ExchangeCodeStatus.USED)
                    .setUserId(UserContext.getUser());
            exchangeCodeService.updateById(exchangeCode1);
        } catch (BizIllegalException e) {
            log.error("兑换码兑换优惠券异常:", e);
            exchangeCodeService.setAndGetStatusForExchangeCode(serialNo, false);
            throw new RuntimeException(e);
        }
    }
}
