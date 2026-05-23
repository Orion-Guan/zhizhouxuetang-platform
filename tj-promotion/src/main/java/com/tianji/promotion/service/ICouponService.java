package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;

import javax.validation.Valid;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-05-17
 */
public interface ICouponService extends IService<Coupon> {

    /**
     * 新增优惠券
     * @param couponFormDTO
     */
    void saveCoupon(CouponFormDTO couponFormDTO);

    /**
     * 查询优惠券分页列表
     * @param couponQuery
     * @return
     */
    PageDTO<CouponPageVO> queryPage(CouponQuery couponQuery);

    /**
     * 发放优惠券
     * @param couponIssueFormDTO
     */
    void issueCoupon(@Valid CouponIssueFormDTO couponIssueFormDTO);
}
