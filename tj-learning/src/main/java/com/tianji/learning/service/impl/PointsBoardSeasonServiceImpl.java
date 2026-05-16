package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
@Service
@Validated
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    @Override
    public Integer getLastSeasonId(LocalDateTime lastDateTime) {
        Optional<PointsBoardSeason> pointsBoardSeason = this.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, lastDateTime)
                .ge(PointsBoardSeason::getEndTime, lastDateTime)
                .oneOpt();
        return pointsBoardSeason.map(PointsBoardSeason::getId).orElse(null);
    }
}
