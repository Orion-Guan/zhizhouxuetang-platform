package com.tianji.api.client.promotion.fallback;

import com.tianji.api.client.promotion.PromotionClient;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

@Slf4j
public class PromotionClientFailback implements FallbackFactory<PromotionClient> {
    @Override
    public PromotionClient create(Throwable cause) {
        log.error("获取优惠方案失败:", cause);
        return new PromotionClient() {
            @Override
            public List<CouponDiscountDTO> getAvailableDiscountSolutions(List<OrderCourseDTO> orderCourseDTOS) {
                return Collections.emptyList();
            }
        };
    }
}
