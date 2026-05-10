package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    /**
     * 查询积分榜单
     * @param pointsBoardQuery
     * @return
     */
    PointsBoardVO queryPointsBoards(PointsBoardQuery pointsBoardQuery);
}
