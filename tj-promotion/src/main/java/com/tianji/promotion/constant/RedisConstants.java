package com.tianji.promotion.constant;


public interface RedisConstants {

    interface PromotionConstants{
        String COUPON_EXCHANGECODE_KEY = "coupon:exchangeCode:serial";

        String EXCHANGE_CODE_STATUS_KEY = "exchangeCode:status:serial";

        String PROMOTION_INFO_KEY_PREFIX = "promotion:id:{}";

        String PROMOTION_USER_COUPON_PREFIX = "promotion:userCoupon:cId:{}";
    }
}
