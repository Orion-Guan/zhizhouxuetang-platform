package com.tianji.promotion.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.math.Calculator;
import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.factory.DiscountStrategyFactory;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponDiscountService;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.strategy.discount.Discount;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponDiscountImpl implements ICouponDiscountService {

    private final UserCouponMapper userCouponMapper;

    private final ICouponScopeService couponScopeService;

    @Resource(name = "promotionExecutor")
    private final Executor executor;

    @Override
    public List<CouponDiscountDTO> findDiscountSolutions(List<OrderCourseDTO> orderCourseDTOS) {
        // 获取用户可用的优惠券
        List<Coupon> coupons = userCouponMapper.getAvailableCoupons(UserContext.getUser());
        if (CollUtil.isEmpty(coupons)) {
            log.error("用户当前无可用优惠券");
            return CollUtil.newArrayList();
        }

        // 计算课程总价
        int prices = orderCourseDTOS.stream().mapToInt(OrderCourseDTO::getPrice).sum();

        // 初步过滤出可用的优惠券
        List<Coupon> availableCoupon = coupons.stream().filter(c -> DiscountStrategyFactory.getDiscount(c.getDiscountType()).canUse(prices, c))
                .collect(Collectors.toList());

        // 获取每种优惠券（前提可用的优惠券）对应可用的课程商品
        Map<Coupon, List<OrderCourseDTO>> couponCourseListMap = getEndAvailableCoupon(availableCoupon, orderCourseDTOS);
        if (couponCourseListMap.isEmpty()) {
            log.error("无可用的优惠券");
            return CollUtil.newArrayList();
        }

        // 全排列组合优惠方案
        ArrayList<Coupon> coupons1 = new ArrayList<>(couponCourseListMap.keySet());
        List<List<Coupon>> promotionSolution = PermuteUtil.permute(coupons1);
        coupons1.forEach(coupon -> {
            promotionSolution.add(List.of(coupon));  // 添加支持用户选择单张优惠券的优惠方案
        });

        // 计算每个优惠方案的金额
        ArrayList<CouponDiscountDTO> list = new ArrayList<CouponDiscountDTO>(promotionSolution.size());
        List<CouponDiscountDTO> discountDTOList = getCouponDiscountDTOList(promotionSolution, couponCourseListMap, orderCourseDTOS);
        list.addAll(discountDTOList);

        // 找出最优的优惠方案
        return getBestGoodSolution(list);
    }

    /**
     * 从所有优惠方案中筛选出最优方案
     * <p>
     * 采用双重最优策略：
     * <ul>
     *   <li>策略1：相同优惠券组合下，选择优惠金额最高的方案</li>
     *   <li>策略2：相同优惠金额下，选择使用优惠券数量最少的方案</li>
     * </ul>
     * 最终返回同时满足两个策略的方案，确保用户获得最佳优惠体验。
     *
     * @param list 所有计算完成的优惠方案列表
     * @return 最优优惠方案列表（可能为空或多个并列最优方案）
     */
    private List<CouponDiscountDTO> getBestGoodSolution(ArrayList<CouponDiscountDTO> list) {
        // 定义使用相同优惠券下，优惠金额最高的方案1
        HashMap<String, CouponDiscountDTO> discountMax = new HashMap<>();
        // 定义相同金额下，使用优惠券最少的方案2
        HashMap<Integer, CouponDiscountDTO> countMin = new HashMap<>();
        //遍历优惠方案
        for (CouponDiscountDTO discountDTO : list) {
            // 获取升序后的优惠券id字符串拼接（不同组合使用相同优惠券结果相同）
            String ascIdsKeyString = discountDTO.getIds().stream().sorted(Long::compareTo).map(String::valueOf).collect(Collectors.joining(","));
            // 选出做优的方案1
            CouponDiscountDTO discountDTO1 = discountMax.get(ascIdsKeyString);
            if (discountDTO1 != null && discountDTO1.getDiscountAmount() >= discountDTO.getDiscountAmount()) {
                continue;
            }
            // 选出做优的方案2
            CouponDiscountDTO discountDTO2 = countMin.get(discountDTO.getDiscountAmount());
            if (discountDTO.getIds().size() > 1 && discountDTO2 != null && discountDTO2.getIds().size() <= discountDTO.getIds().size()) {
                continue;
            }
            // 以上都满足的话说明此优惠方案最优
            discountMax.put(ascIdsKeyString, discountDTO);
            countMin.put(discountDTO.getDiscountAmount(), discountDTO);
        }
        // 取两者的交集即为最优解
        discountMax.values().retainAll(countMin.values());

        return discountMax.values().stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }

    private List<CouponDiscountDTO> getCouponDiscountDTOList(List<List<Coupon>> promotionSolution, Map<Coupon, List<OrderCourseDTO>> couponCourseListMap, List<OrderCourseDTO> orderCourseDTOS) {
        // 使用线程安全的集合（防止并发带来的列表元素被覆盖、元素个数来回变化问题）
        List<CouponDiscountDTO> list = Collections.synchronizedList(new ArrayList<>(promotionSolution.size()));
        //获取倒计数锁存器
        CountDownLatch countDownLatch = new CountDownLatch(promotionSolution.size());
        // 循环遍历每个折扣方案
        promotionSolution.forEach(couponList -> {
            CompletableFuture.supplyAsync(() -> calculateDiscountPlan(couponCourseListMap, orderCourseDTOS, couponList), executor)
                    .thenAccept(cd -> {
                        list.add(cd);  // 并发同时向列表中写数据---注意线程安全问题
                        countDownLatch.countDown();  // 计数器减一
                    });

        });
        // 阻塞等待其他线程都执行完了，才能将最终的优惠方案返回，否则返回列表可能无元素（多线程异步并行计算优惠方案）
        try {
            boolean await = countDownLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("计算优惠方案超时:", e);
        }
        return list;
    }

    private CouponDiscountDTO calculateDiscountPlan(Map<Coupon, List<OrderCourseDTO>> couponCourseListMap, List<OrderCourseDTO> orderCourseDTOS, List<Coupon> couponList) {
        CouponDiscountDTO discountDTO = new CouponDiscountDTO();
        //定义课程商品对应的优惠券叠加折扣明细
        Map<Long, Integer> discountDetailByCourseIdMap = orderCourseDTOS.stream().collect(Collectors.toMap(OrderCourseDTO::getId, orderCourseDTO -> 0));
        //当前优惠方案中每个优惠券对课程的优惠
        for (Coupon coupon : couponList) {
            //获取当前优惠券适用的本次订单课程
            List<OrderCourseDTO> courseDTOS = couponCourseListMap.get(coupon);
            //计算课程总价
            int amount = courseDTOS.stream().mapToInt(course -> course.getPrice() - discountDetailByCourseIdMap.get(course.getId())).sum();
            //判断是否可用
            Discount discount = DiscountStrategyFactory.getDiscount(coupon.getDiscountType());
            if (!discount.canUse(amount, coupon)) {
                continue;
            }
            //计算优惠金额
            int discountAmount = discount.calculateDiscount(amount, coupon);
            //记录当前优惠总金额针对各课程的优惠明细(方便本组优惠券方案的下个优惠券做判断)
            calculateDetailForEachCourses(discountDetailByCourseIdMap, discountAmount, courseDTOS, amount);
            discountDTO.getIds().add(coupon.getId());
            discountDTO.getRules().add(discount.getRule(coupon));
            discountDTO.setDiscountAmount(discountDTO.getDiscountAmount() + discountAmount);
        }
        return discountDTO;
    }

    private void calculateDetailForEachCourses(Map<Long, Integer> discountDetailByCourseIdMap,
                                               int discountAmount,
                                               List<OrderCourseDTO> courseDTOS,
                                               int discountCoursesPrices) {
        int times = 0;
        int remainAmount = discountAmount;
        // 计算每个课程折扣明细
        for (OrderCourseDTO courseDTO : courseDTOS) {
            times++;
            if (times == courseDTOS.size()) {
                discountDetailByCourseIdMap.put(courseDTO.getId(), discountDetailByCourseIdMap.get(courseDTO.getId()) + remainAmount);
            } else {
                // 获取当前课程被之前优惠券折扣后价格
                int currPrices = courseDTO.getPrice() - discountDetailByCourseIdMap.get(courseDTO.getId());
                // 计算当前优惠券对本次课程的折扣明细
                int discountAmountForCourse = currPrices / discountCoursesPrices * discountAmount;
                // 将当前课程的折扣值累加保存起来
                discountDetailByCourseIdMap.put(courseDTO.getId(), discountDetailByCourseIdMap.get(courseDTO.getId()) + discountAmountForCourse);
                remainAmount -= discountAmountForCourse;
            }

        }
    }


    private Map<Coupon, List<OrderCourseDTO>> getEndAvailableCoupon(List<Coupon> availableCoupon, List<OrderCourseDTO> orderCourseDTOS) {
        HashMap<Coupon, List<OrderCourseDTO>> couponListHashMap = new HashMap<>(availableCoupon.size());
        // 获取优惠券对应可使用的课程
        for (Coupon coupon : availableCoupon) {
            List<OrderCourseDTO> orderCourseDTOS1 = orderCourseDTOS;
            if (coupon.getSpecific()) {
                List<CouponScope> couponScopeList = couponScopeService.lambdaQuery().eq(CouponScope::getCouponId, coupon.getId()).list();
                Set<Long> bizIdSet = couponScopeList.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
                orderCourseDTOS1 = orderCourseDTOS1.stream().filter(oc -> bizIdSet.contains(oc.getCateId())).collect(Collectors.toList());
            }
            // 判断当前优惠券是否可用（条件1）
            if (CollUtil.isEmpty(orderCourseDTOS1)) {
                log.info("当前优惠券不适用此订单中的课程");
                continue;
            }
            // 判断当前优惠券是否可用（条件2）
            int sum = orderCourseDTOS1.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            Discount discount = DiscountStrategyFactory.getDiscount(coupon.getDiscountType());
            if (discount.canUse(sum, coupon)) {
                couponListHashMap.put(coupon, orderCourseDTOS1);
            }
        }
        return couponListHashMap;
    }
}
