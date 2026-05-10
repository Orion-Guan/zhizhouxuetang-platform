package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.contants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */

@Service
@Validated  //启用方法参数效验
@Slf4j
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 添加用户积分记录
     * <p>
     * 该方法会根据积分类型的上限规则检查用户今日已获得的积分，
     * 如果超过上限则调整实际可添加的积分数。若今日已达上限，则不添加积分记录。
     * </p>
     *
     * @param userId 用户ID，不能为空
     * @param pointsRecordType 积分记录类型，不能为空，包含积分上限规则
     * @param points 要添加的积分数值，不能为空且最小值为1
     */
    @Override
    public void addPointsRecord(@NotNull Long userId, @NotNull PointsRecordType pointsRecordType, @NotNull @Min(1L) Integer points) {
        // 判断此方式获取的积分是否有积分上限
        Integer realPoints = points;
        LocalDateTime now = LocalDateTime.now();
        if(pointsRecordType.getMaxPoints() > 0 ){
            // 获取用户今日已得积分
            LocalDateTime startTime = DateUtils.getDayStartTime(now);
            LocalDateTime endTime = DateUtils.getDayEndTime(now);
            LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<PointsRecord>()
                    .eq(PointsRecord::getUserId, userId)
                    .eq(PointsRecord::getType, pointsRecordType)
                    .between(PointsRecord::getCreateTime, startTime, endTime);
            Integer currPoints = getBaseMapper().getPointCurrentUserById(wrapper);
            if(currPoints == null){
                currPoints = 0;
            }
            if(currPoints >= pointsRecordType.getMaxPoints()){
                log.warn("今日获取积分已超上限！");
                return;
            }
            if(currPoints + points > pointsRecordType.getMaxPoints()){
                realPoints = pointsRecordType.getMaxPoints() - currPoints;
            }
        }

        // 保存积分记录
        PointsRecord pointsRecord = new PointsRecord().setUserId(userId).setType(pointsRecordType).setPoints(realPoints);
        save(pointsRecord);
        log.debug("保存积分记录成功: {}", JSONUtil.toJsonStr(pointsRecord));

        // 累加用户当月获得的积分到redis中（提升高并发下当月积分排行榜查询效率）
        String yearMonth = DateUtils.format(now,DateUtils.USER_POINTS_RANKING_DATE_SUFFIX_FORMATTER);
        String key = StrUtil.format(RedisConstants.USER_POINTS_RANKING_PREFIX,yearMonth);
        stringRedisTemplate.opsForZSet().incrementScore(key,userId.toString(),realPoints);
        log.debug("累加用户积分成功: {}",key);
    }

    @Override
    public List<PointsStatisticsVO> queryUserPointsOfDay() {
        //获取当前用户
        Long userId = UserContext.getUser();

        //获取今日起止时间
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        //获取用户今日每种类型总积分
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId,userId)
                .between(PointsRecord::getCreateTime,startTime,endTime);
        List<PointsRecord> pointsRecords = getBaseMapper().queryUserPointsOfDay(wrapper);
        if(CollUtil.isEmpty(pointsRecords)){
            log.warn("用户今日无积分记录！");
            return Collections.emptyList();
        }

        //组装返回数据
        ArrayList<PointsStatisticsVO> pointsStatisticsVOS = new ArrayList<>(pointsRecords.size());
        for (PointsRecord pointsRecord : pointsRecords) {
            PointsStatisticsVO vo = PointsStatisticsVO.builder()
                    .type(pointsRecord.getType().getDesc())
                    .points(pointsRecord.getPoints())
                    .maxPoints(pointsRecord.getType().getMaxPoints())
                    .build();
            pointsStatisticsVOS.add(vo);
        }
        return pointsStatisticsVOS;
    }
}
