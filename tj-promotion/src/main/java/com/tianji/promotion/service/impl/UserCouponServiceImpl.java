package com.tianji.promotion.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.autoconfigure.redisson.annotations.Lock;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constant.RedisConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.DistributedLockType;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static com.tianji.promotion.constant.RedisConstants.PromotionConstants.PROMOTION_INFO_KEY_PREFIX;
import static com.tianji.promotion.constant.RedisConstants.PromotionConstants.PROMOTION_USER_COUPON_PREFIX;

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
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;

    private final IExchangeCodeService exchangeCodeService;

    private final StringRedisTemplate stringRedisTemplate;

    private final RabbitMqHelper rabbitMqHelper;

    @Override
    @Lock(name = "lock:promotion:id:#{couponId}")
    public void receiveCoupon(Long couponId) throws InterruptedException {
        // 查询优惠券(从缓存中查)
        Coupon coupon = getCouponCacheById(couponId);

        //效验发放领取时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
            log.error("不在领取时间范围内");
            throw new BizIllegalException("不在领取时间范围内");
        }

        // 效验库存(用户每次领取成功后缓存中的此字段会自减)
        if (coupon.getTotalNum() <= 0) {
            log.error("优惠券库存不足：{}", couponId);
            throw new BizIllegalException("优惠券库存不足");
        }

        // 效验用户领取限额(从缓存中读取: 只是效验是否超限，出现异常时无需回滚处理)
        Long afterCount = stringRedisTemplate.opsForHash().increment(StrUtil.format(PROMOTION_USER_COUPON_PREFIX, couponId), UserContext.getUser().toString(), 1);
        if (afterCount > coupon.getUserLimit()) {
            log.error("{}用户领取优惠券超限：{}", UserContext.getUser(), couponId);
            throw new BizIllegalException("用户领取优惠券超限");
        }

        // 效验全通过: 扣减缓存优惠券库存
        stringRedisTemplate.opsForHash().increment(
                StrUtil.format(PROMOTION_INFO_KEY_PREFIX, couponId),
                "totalNum",
                -1
        );

        // 异步发送ＭQ消息通知给消费者：添加库存可领取记录
        UserCouponDTO userCouponDTO = new UserCouponDTO();
        userCouponDTO.setUserId(UserContext.getUser());
        userCouponDTO.setCouponId(couponId);
        rabbitMqHelper.send(MqConstants.Exchange.USER_COUPON_EXCHANGE,MqConstants.Key.USER_COUPON_KEY,userCouponDTO);

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

        /*// 获取锁对象: 防止单用户多次并发领取优惠券导致超限问题
        String key = "lock:promotion:uid:" + UserContext.getUser();
        RLock lock = redissonClient.getLock(key);
        boolean success = lock.tryLock(3, 30, TimeUnit.SECONDS);  //3：表示未获取到锁则排队3秒重试获取锁（过期时间即第二个参数为-1L时：底层会触发看门狗守护线程-重置key超时时间，每30/3=10秒）
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
        }*/

        /*// 通过获取UserCouponServiceImpl代理对象，来调用其事务方法
        UserCouponServiceImpl userCouponServiceAgency = (UserCouponServiceImpl) AopContext.currentProxy();
        userCouponServiceAgency.checkLimitAndSaveUserCoupon(coupon, now, UserContext.getUser());*/
    }

    private Coupon getCouponCacheById(Long couponId) {
        Map<Object, Object> couponInfo = stringRedisTemplate.opsForHash().entries(StrUtil.format(PROMOTION_INFO_KEY_PREFIX, couponId));
        if (CollUtil.isEmpty(couponInfo)) {
            log.error("优惠券不存在!");
            throw new BizIllegalException("优惠券不存在!");
        }
        return JSONUtil.parseObj(couponInfo).toBean(Coupon.class);
    }


    @Transactional(rollbackFor = Exception.class)
    public void checkLimitAndSaveUserCoupon(UserCouponDTO userCouponDTO) {
        Coupon coupon = couponMapper.selectById(userCouponDTO.getCouponId());
        if(null == coupon){
            throw new BizIllegalException("优惠券不存在");
        }

        //自增优惠券库存
        int rows = couponMapper.increaseIssueNum(userCouponDTO.getCouponId());
        if (rows < 1) {
            throw new BizIllegalException("优惠券库存不足");
        }

        //新增数据
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (null == termBeginTime || null == termEndTime) {
            termBeginTime = LocalDateTime.now();
            termEndTime = LocalDateTime.now().plusDays(coupon.getTermDays());
        }
        UserCoupon userCoupon = new UserCoupon()
                .setUserId(userCouponDTO.getUserId())
                .setCouponId(userCouponDTO.getCouponId())
                .setStatus(UserCouponStatus.UNUSED)
                .setCreateTime(LocalDateTime.now())
                .setTermBeginTime(termBeginTime)
                .setTermEndTime(termEndTime);
        this.save(userCoupon);
    }


    /**
     * 优化方案一：
     * 1、效验使用redis, 发放基于MQ。其中根据兑换码查询优惠券，可以在生成兑换码后，使用有序集合其中成员是优惠券id，得分是优惠券的最大序号。
     * 用户在查询兑换码所属的优惠券时，直接通过range命令查找 "1--1+优惠券最大上限"分数的成员，然后根据分数升序排序获取首个成员即为兑换码所属的优惠券
     *
     * 优化方案二： 使用Redis的Lua脚本
     * @param code
     */
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
            //todo: checkLimitAndSaveUserCoupon(coupon, now, UserContext.getUser());
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
