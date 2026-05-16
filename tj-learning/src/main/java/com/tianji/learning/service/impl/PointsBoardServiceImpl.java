package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.contants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-25
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate stringRedisTemplate;

    private final UserClient userClient;

    /**
     * 查询学霸天梯榜数据，支持当前赛季和历史赛季（实时榜单从redis中查询，历史赛季榜单从mysql中查询）
     *
     * @param pointsBoardQuery 查询条件，包含赛季、页码、每页数量等信息
     * @return 榜单数据，包含我的积分排名和榜单明细列表
     */
    public PointsBoardVO queryPointsBoards(PointsBoardQuery pointsBoardQuery) {
        // 判断查询的是当前月份的赛季榜单还是历史榜单
        boolean isCurrent = pointsBoardQuery.getSeason() == null || pointsBoardQuery.getSeason() == 0L;

        // 查询我的积分和排名
        Long userId = UserContext.getUser();
        String currentYearMonth = LocalDate.now().format(DateUtils.USER_POINTS_RANKING_DATE_SUFFIX_FORMATTER);
        String key = StrUtil.format(RedisConstants.USER_POINTS_RANKING_PREFIX, currentYearMonth);
        PointsBoard myPointsBoard = isCurrent ? queryCurrentMyBoard(userId.toString(), key)
                : queryHistoryMyBoard(userId, pointsBoardQuery.getSeason());

        // 分页查询用户榜单集合
        List<PointsBoard> pointsBoardList = isCurrent ? queryCurrentBoard(key, pointsBoardQuery.getPageNo(), pointsBoardQuery.getPageSize())
                : queryHistoryBoard(pointsBoardQuery);
        
        // 封装返回数据
        PointsBoardVO pointsBoardVO = new PointsBoardVO();
        pointsBoardVO.setPoints(myPointsBoard != null && myPointsBoard.getPoints() != null? myPointsBoard.getPoints():0);
        pointsBoardVO.setRank(myPointsBoard != null && myPointsBoard.getRank() != null? myPointsBoard.getRank():0);
        
        // 获取用户信息并组装榜单明细
        if(CollUtil.isEmpty(pointsBoardList)){
            return pointsBoardVO;
        }
        
        Set<Long> userIdSet = pointsBoardList.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> userDTOList = userClient.queryUserByIds(userIdSet);
        Map<Long, String> hashMapByUserId = new HashMap<>();
        if(CollUtil.isNotEmpty(userDTOList)){
            hashMapByUserId = userDTOList.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }

        ArrayList<PointsBoardItemVO> pointsBoardItemVOS = new ArrayList<>(pointsBoardList.size());
        for (PointsBoard pointsBoard : pointsBoardList) {
            PointsBoardItemVO pointsBoardItemVO = new PointsBoardItemVO();
            pointsBoardItemVO.setName(hashMapByUserId.get(pointsBoard.getUserId()));
            pointsBoardItemVO.setPoints(pointsBoard.getPoints());
            pointsBoardItemVO.setRank(pointsBoard.getRank());
            pointsBoardItemVOS.add(pointsBoardItemVO);
        }
        pointsBoardVO.setBoardList(pointsBoardItemVOS);
        return pointsBoardVO;
    }

    /**
     * 创建历史赛季的积分榜分表结构
     * @param tableName
     */
    @Override
    public void createTableOfHistoryBoard(String tableName) {
        this.getBaseMapper().createTable(tableName);
    }


    /**
     * 查询指定赛季的积分榜
     * @param pointsBoardQuery
     * @return
     */
    private List<PointsBoard> queryHistoryBoard(PointsBoardQuery pointsBoardQuery) {
        return null;
    }


    /**
     * 查询当前月份积分榜分页数据
     *
     * @param key      Redis中存储积分榜的ZSet键名
     * @param pageNo   页码，从1开始
     * @param pageSize 每页查询数量
     * @return 积分榜用户列表，包含用户ID、积分和排名信息
     */
    @Override
    public List<PointsBoard> queryCurrentBoard(String key, Integer pageNo, Integer pageSize) {
        // 计算开始位置索引
        int from = (pageNo - 1) * pageSize;

        // 从Redis中查询当前月份积分榜
        Set<ZSetOperations.TypedTuple<String>> tupleSet = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, from, from + pageSize - 1);

        // 封装返回数据
        AtomicReference<Integer> rank = new AtomicReference<>(from + 1);
        
        if (CollUtil.isEmpty(tupleSet)) {
            return Collections.emptyList();
        }

        // 将Redis ZSet结果转换为PointsBoard对象列表，并计算排名
        return tupleSet.stream().map(item -> {
                    String userId = item.getValue();
                    Double score = item.getScore();
                    if (null != userId && null != score) {
                        return new PointsBoard()
                                .setUserId(Long.valueOf(userId))
                                .setPoints(score.intValue())
                                .setRank(rank.getAndSet(rank.get() + 1));
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 查询当前用户指定赛季的积分和排名
     *
     * @param userId
     * @param season
     * @return
     */
    private PointsBoard queryHistoryMyBoard(Long userId, Long season) {
        return null;
    }


    /**
     * 查询当前月份用户的积分和排名
     *
     * @param userId
     * @param key
     * @return
     */
    private PointsBoard queryCurrentMyBoard(String userId, String key) {
        //从Redis有序集合中查询当前月份用户的得分和排名
        BoundZSetOperations<String, String> ops = stringRedisTemplate.boundZSetOps(key);
        Double score = ops.score(userId);
        Long rank = ops.reverseRank(userId);
        return new PointsBoard()
                .setRank(rank != null ? Math.toIntExact(rank) + 1: 0)
                .setPoints(score != null ? score.intValue() : 0);
    }
}
