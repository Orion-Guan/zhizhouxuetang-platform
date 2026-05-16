package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.po.PointsBoard;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    /**
     * 创建积分榜表
     * @param tableName
     */
    void createTable(String tableName);
}
