package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.List;

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


    /**
     * 创建历史赛季积分榜单分表结构
     * @param tableName
     */
    void createTableOfHistoryBoard(@NotEmpty String tableName);

    /**
     * 查询当前月份积分榜分页数据
     * @param key
     * @param pageNo
     * @param pageSize
     * @return
     */
    List<PointsBoard> queryCurrentBoard(String key, @Min(value = 1, message = "页码不能小于1") Integer pageNo, @Min(value = 1, message = "每页查询数量不能小于1") Integer pageSize);
}
