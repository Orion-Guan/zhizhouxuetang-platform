package com.tianji.promotion.factory;

import com.tianji.promotion.enums.DiscountType;
import com.tianji.promotion.strategy.discount.*;

import java.util.EnumMap;


/**
 * 折扣策略工厂类
 * 使用策略模式管理不同类型的折扣计算策略
 * 通过EnumMap实现高效的策略查找和分发
 */
public class DiscountStrategyFactory {

    /** 折扣策略映射表，key为折扣类型，value为对应的折扣策略实现 */
    private final static EnumMap<DiscountType, Discount> strategies;

    // 静态代码块：初始化所有折扣策略实例
    static {
        strategies = new EnumMap<>(DiscountType.class);
        strategies.put(DiscountType.NO_THRESHOLD, new NoThresholdDiscount()); // 无门槛折扣策略
        strategies.put(DiscountType.PER_PRICE_DISCOUNT, new PerPriceDiscount()); // 每满减折扣策略
        strategies.put(DiscountType.RATE_DISCOUNT, new RateDiscount()); // 折扣率策略
        strategies.put(DiscountType.PRICE_DISCOUNT, new PriceDiscount()); // 固定价格折扣策略
    }

    /**
     * 根据折扣类型获取对应的折扣策略实例
     *
     * @param type 折扣类型枚举
     * @return 对应的折扣策略实现
     */
    public static Discount getDiscount(DiscountType type) {
        return strategies.get(type);
    }
}