package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.DiscountType;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.tianji.promotion.constant.RedisConstants.PromotionConstants.PROMOTION_INFO_KEY_PREFIX;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-05-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final ICouponScopeService iCouponScopeService;

    private final IExchangeCodeService iExchangeCodeService;

    private final IUserCouponService iUserCouponService;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveCoupon(CouponFormDTO couponFormDTO) {
        //保存优惠卷
        Coupon coupon = BeanUtil.toBean(couponFormDTO, Coupon.class);
        this.save(coupon);
        if (!couponFormDTO.getSpecific()) {
            return;
        }

        //保存使用范围到中间表
        List<Long> scopes = couponFormDTO.getScopes();
        if (CollUtil.isEmpty(scopes)) {
            log.error("指定限定范围: {}", JSONUtil.toJsonStr(couponFormDTO));
            throw new BadRequestException("限定范围需要指定其限定的数据！");
        }
        List<CouponScope> scopeList = scopes.stream()
                .map(bizId -> new CouponScope()
                        .setType(1)
                        .setCouponId(coupon.getId())
                        .setBizId(bizId))
                .collect(Collectors.toList());
        iCouponScopeService.saveBatch(scopeList);
    }

    @Override
    public PageDTO<CouponPageVO> queryPage(CouponQuery couponQuery) {
        Integer discountType = couponQuery.getType();
        String name = couponQuery.getName();
        Integer status = couponQuery.getStatus();
        Integer pageNo = couponQuery.getPageNo();
        Integer pageSize = couponQuery.getPageSize();

        //分页查询数据
        Page<Coupon> page = this.lambdaQuery()
                .eq(null != discountType, Coupon::getDiscountType, discountType)
                .like(StrUtil.isNotEmpty(name), Coupon::getName, name)
                .eq(null != status, Coupon::getStatus, status)
                .page(Page.<Coupon>of(pageNo, pageSize).addOrder(OrderItem.desc("create_time")));  //显式指定泛型，避免编译器无法推断出具体的类型

        //封装Vo
        List<Coupon> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        List<CouponPageVO> couponPageVOS = BeanUtil.copyToList(records, CouponPageVO.class);

        //返回数据
        return PageDTO.of(page, couponPageVOS);
    }

    /**
     * 发放优惠券，根据发放开始时间判断是立即发放还是定时发放
     *
     * @param couponIssueFormDTO 优惠券发放表单DTO，包含优惠券ID、发放开始时间、发放结束时间等信息
     */
    @Override
    @Transactional
    public void issueCoupon(CouponIssueFormDTO couponIssueFormDTO) {
        // 查询优惠券获取其状态
        Coupon coupon = this.getById(couponIssueFormDTO.getId());
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
            log.error("{}状态下的优惠券不允许发放", coupon.getStatus());
            throw new BizIllegalException("当前状态下的优惠券不允许发放");
        }

        //效验参数
        LocalDateTime issueBeginTime = couponIssueFormDTO.getIssueBeginTime();
        LocalDateTime issueEndTime = couponIssueFormDTO.getIssueEndTime();
        if (!issueEndTime.isAfter(LocalDateTime.now()) || (issueBeginTime != null && !issueEndTime.isAfter(issueBeginTime))) {
            log.error("优惠券发放开始时间和结束时间有误");
            throw new BizIllegalException("优惠券方放开始时间和结束时间有误");
        }

        // 判断优惠券发放方式：1、立即发放->进行中  2、 定时发放->未开始
        Coupon coupon1 = BeanUtil.toBean(couponIssueFormDTO, Coupon.class);
        boolean isBegin = issueBeginTime == null || (!issueBeginTime.isAfter(LocalDateTime.now()));
        if (isBegin) {
            coupon1.setStatus(CouponStatus.ISSUING)
                    .setIssueBeginTime(LocalDateTime.now());  //优惠券无发放开始时间或发放开始时间在当前时间之前，依然使用当前时间
        } else {
            coupon1.setStatus(CouponStatus.UN_ISSUE);
        }

        //更新优惠券数据
        this.updateById(coupon1);

        // 立即发放优惠券--缓存优惠券数据
        if (isBegin) {
            coupon.setIssueBeginTime(coupon1.getIssueBeginTime());
            coupon.setIssueEndTime(coupon1.getIssueEndTime());
            cacheCoupon(coupon);
        }

        //异步生成兑换码（只有是待发放状态且是指定发放方式）
        if (coupon.getStatus() == CouponStatus.DRAFT && coupon.getObtainWay() == ObtainType.ISSUE) {
            coupon.setIssueEndTime(couponIssueFormDTO.getIssueEndTime());
            iExchangeCodeService.generateExchangeCodeByAsync(coupon);
        }
    }


    /**
     * 缓存优惠券信息到Redis中
     * @param coupon1 需要缓存的优惠券对象
     */
    private void cacheCoupon(Coupon coupon1) {
        // 构建Redis中的键，使用优惠券ID作为唯一标识
        String key = StrUtil.format(PROMOTION_INFO_KEY_PREFIX, coupon1.getId());
        // 将优惠券开始时间转换为时间戳字符串
        String startTime = String.valueOf(DateUtils.toEpochMilli(coupon1.getIssueBeginTime()));
        String endTime = String.valueOf(DateUtils.toEpochMilli(coupon1.getIssueEndTime()));
        Map<String, ? extends Serializable> stringMap = Map.of(
                "issueBeginTime", startTime,
                "issueEndTime", endTime,
                "totalNum", String.valueOf(coupon1.getTotalNum()),
                "userLimit", String.valueOf(coupon1.getUserLimit())
        );
        stringRedisTemplate.opsForHash().putAll(key, stringMap);
    }

    @Override
    public List<CouponVO> couponList() {
        //获取手动领取进行中的优惠券
        List<Coupon> list = this.lambdaQuery()
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .list();
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        //获取用户每张优惠券领取的数量
        Set<Long> couponIdSet = list.stream().map(Coupon::getId).collect(Collectors.toSet());
        List<UserCoupon> userCouponList = iUserCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIdSet)
                .list();
        if (CollUtil.isEmpty(userCouponList)) {
            userCouponList = new ArrayList<>();
        }
        Map<Long, Long> couponCountMapByCouponId = userCouponList.stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        //获取用户已领取且未使用的优惠券数量
        Map<Long, Long> couponCountUnUsedMapByCouponId = userCouponList.stream()
                .filter(userCoupon -> userCoupon.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        //封装返回数据
        return list.stream().map(coupon -> {
            CouponVO couponVO = BeanUtil.toBean(coupon, CouponVO.class);
            couponVO.setAvailable(coupon.getIssueNum() < coupon.getTotalNum() && couponCountMapByCouponId.getOrDefault(coupon.getId(), 0L) < coupon.getUserLimit());
            couponVO.setReceived(couponCountUnUsedMapByCouponId.getOrDefault(coupon.getId(), 0L) > 0);
            return couponVO;
        }).collect(Collectors.toList());
    }
}
