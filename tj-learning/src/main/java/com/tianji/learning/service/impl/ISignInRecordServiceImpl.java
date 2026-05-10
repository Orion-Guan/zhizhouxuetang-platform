package com.tianji.learning.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.contants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignedMessage;
import com.tianji.learning.service.ISignInRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ISignInRecordServiceImpl implements ISignInRecordService {

    private final StringRedisTemplate redisTemplate;

    private final RabbitMqHelper rabbitMqHelper;


    @Override
    public SignResultVO addSignRecord() {
        //添加签到记录
        Long userId = UserContext.getUser();
        String formatDate = DateUtil.format(LocalDateTime.now(), DateUtils.DEFAULT_MONTH_FORMAT_COMPACT);
        String key = StrUtil.format(RedisConstants.SIGN_RECORD_PREFIX,userId,formatDate);
        int day = LocalDateTime.now().getDayOfMonth() - 1;
        Boolean isExist = redisTemplate.opsForValue().setBit(key, day,true);

        //检测用户是否重复签到
        if(BooleanUtil.isTrue(isExist)){
            log.error("{}用户重复签到",userId);
            throw new BizIllegalException("用户重复签到");
        }

        //计算连续签到天数和积分
        int signDayCounts  = getSignDays(key, LocalDate.now().getDayOfMonth());

        //获取连续签到奖励的积分
        int rewardPoint = 0;
        switch (signDayCounts){
            case 7:
                rewardPoint = 10;
                break;
            case 14:
                rewardPoint = 20;
                break;
            case 28:
                rewardPoint = 40;
                break;
            default:
                log.debug("不奖励额外积分:不满足连续签到规定的天数");
        }

        // 添加用户获取的签到积分
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,MqConstants.Key.SIGN_IN, SignedMessage.of(userId,1 + rewardPoint));

        //封装返回结果
        SignResultVO signResultVO = new SignResultVO();
        signResultVO.setSignDays(signDayCounts);
        signResultVO.setSignPoints(1);
        signResultVO.setRewardPoints(rewardPoint);
        return signResultVO;
    }


    /**
     * 计算用户连续签到天数
     * 通过Redis位图获取当月签到记录，从当前日期向前统计连续签到的天数
     *
     * @param key Redis中存储签到记录的键
     * @param day 当前日期在月份中的索引（从0开始）
     * @return 连续签到天数
     */
    private int getSignDays(String key, int day) {
        // 获取截止到今天学员所有的签到记录
        List<Long> signRecordList = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if(CollUtils.isEmpty(signRecordList) || signRecordList.get(0) == null){
            log.info("未获取到{}学员签到天数:{}",UserContext.getUser(),signRecordList);
            return 0;
        }
        int signRecordNum = signRecordList.get(0).intValue();
        
        // 从签到记录的最后一天连续向前统计用户的连续签到天数
        int count = 0;
        while ((signRecordNum & 1) == 1){
            ++count;
            signRecordNum >>>= 1;
        }
        
        return count;
    }

    @Override
    public List<Integer> getSignRecords() {
        //获取当前用户
        Long userId = UserContext.getUser();
        if (userId == null) {
            log.error("无法获取签到记录，请登录！");
            return new ArrayList<>();
        }
        
        //统一获取当前时间，避免跨天问题
        LocalDate now = LocalDate.now();
        //获取当前月份
        String formatDate = DateUtil.format(now.atStartOfDay(), DateUtils.DEFAULT_MONTH_FORMAT_COMPACT);
        String key = StrUtil.format(RedisConstants.SIGN_RECORD_PREFIX, userId, formatDate);
        //获取今日是本月第几天
        int dayOfMonth = now.getDayOfMonth();

        //查询当月第一天到今日的签到记录 - 使用BITFIELD一次性获取
        List<Integer> signRecords = new ArrayList<>();
        List<Long> results = redisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));

        if (CollUtils.isNotEmpty(results) && results.get(0) != null) {
            long bitMap = results.get(0);
            // 从高位到低位解析每一位的签到状态（第1天在最高位）
            for (int i = dayOfMonth - 1; i >= 0; i--) {
                signRecords.add((int) (bitMap & 1));
                bitMap >>>= 1;
            }
            // 反转列表，使第1天在前，最后一天在后
            CollUtils.reverse(signRecords);
        } else {
            // 如果没有数据，全部填充0
            for (int i = 0; i < dayOfMonth; i++) {
                signRecords.add(0);
            }
        }
        return signRecords;
    }

}
