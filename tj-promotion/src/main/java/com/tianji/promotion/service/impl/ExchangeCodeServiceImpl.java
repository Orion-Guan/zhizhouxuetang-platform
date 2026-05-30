package com.tianji.promotion.service.impl;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import groovy.transform.TailRecursive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import static com.tianji.promotion.constant.RedisConstants.PromotionConstants.COUPON_EXCHANGECODE_KEY;
import static com.tianji.promotion.constant.RedisConstants.PromotionConstants.EXCHANGE_CODE_STATUS_KEY;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-05-17
 */
@Service
@Slf4j
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final BoundValueOperations<String, String> boundValueOps;

    private final ValueOperations<String, String> opsForValue;

    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        opsForValue = redisTemplate.opsForValue();
        boundValueOps = redisTemplate.boundValueOps(COUPON_EXCHANGECODE_KEY);
    }

    /**
     * 异步批量生成兑换码
     * <p>
     * 该方法通过Redis原子递增操作获取全局唯一序列号，基于优惠券信息批量生成兑换码并持久化到数据库。
     * 采用分批处理策略，避免单次处理数据量过大导致内存溢出或数据库压力过大。
     * </p>
     *
     * @param coupon 优惠券对象，包含兑换码数量、有效期、优惠券ID等关键信息
     */
    @Override
    @Async("generateExchangeCodeExecutor")
    @Transactional
    public void generateExchangeCodeByAsync(Coupon coupon) {
        /*
         * 通过Redis原子递增操作获取当前最大序列号
         * increment参数为需要生成的兑换码总数，确保一次性预留足够的序列号空间
         */
        Integer totalNum = coupon.getTotalNum();
        Long maxSerial = boundValueOps.increment(totalNum);
        if (maxSerial == null) {
            log.error("生成兑换码有误：无法从redis中获取最大序列号");
            return;
        }

        // 计算本批次兑换码的起始序列号
        long startSerial = maxSerial - totalNum + 1;

        /*
         * 分批生成兑换码并批量入库
         * 每批处理capacity个兑换码，降低单次数据库操作压力
         */
        int capacity = 10;
        int times = totalNum % capacity > 0 ? totalNum / capacity + 1 : totalNum / capacity;
        for (int i = 1; i <= times; i++) {
            ArrayList<ExchangeCode> exchangeCodes = new ArrayList<>(capacity);

            /*
             * 生成当前批次的兑换码列表
             * 最后一批需要特殊处理，避免超出总数量限制
             */
            for (int j = Math.toIntExact(startSerial); j <= startSerial - 1 + capacity; j++) {
                if (Objects.equals(i, times) && totalNum % capacity > 0 && j > maxSerial) {
                    break;
                }
                String code = CodeUtil.generateCode(j, coupon.getId());
                ExchangeCode exchangeCode1 = new ExchangeCode()
                        .setId(j)
                        .setCode(code)
                        .setStatus(ExchangeCodeStatus.UNUSED)
                        .setType(1)
                        .setExchangeTargetId(coupon.getId())
                        .setCreateTime(LocalDateTime.now())
                        .setExpiredTime(coupon.getIssueEndTime());
                exchangeCodes.add(exchangeCode1);
            }

            // 批量保存当前批次的兑换码到数据库
            this.saveBatch(exchangeCodes);

            // 更新下一批次的起始序列号
            startSerial = Long.valueOf(exchangeCodes.get(exchangeCodes.size() - 1).getId()) + 1;
        }
    }

    @Override
    public Boolean setAndGetStatusForExchangeCode(long serialNo, boolean i) {
        return opsForValue.setBit(EXCHANGE_CODE_STATUS_KEY, serialNo, i);
    }


}
