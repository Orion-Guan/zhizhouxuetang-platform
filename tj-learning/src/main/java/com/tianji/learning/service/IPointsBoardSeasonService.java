package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    /**
     * 获取指定时间之前的赛季id
     * @param lastDateTime
     * @return
     */
    Integer getLastSeasonId(@NotNull LocalDateTime lastDateTime);
}
