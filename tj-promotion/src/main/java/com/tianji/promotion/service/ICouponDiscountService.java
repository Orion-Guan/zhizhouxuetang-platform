package com.tianji.promotion.service;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ICouponDiscountService {

    /**
     * 计算优惠券折扣方案
     * @param orderCourseDTOS
     * @return
     */
    List<CouponDiscountDTO> findDiscountSolutions(List<OrderCourseDTO> orderCourseDTOS);
}
