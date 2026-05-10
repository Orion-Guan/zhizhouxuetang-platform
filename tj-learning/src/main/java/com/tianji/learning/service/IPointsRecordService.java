package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
public interface IPointsRecordService extends IService<PointsRecord> {
    /**
     * 添加积分记录
     * @param userId
     * @param pointsRecordType
     * @param points
     */
    void addPointsRecord(@NotNull Long userId, @NotNull PointsRecordType pointsRecordType, @NotNull @Min(1L) Integer points);

    /**
     * 查询用户积分
     * @return
     */
    List<PointsStatisticsVO> queryUserPointsOfDay();
}
