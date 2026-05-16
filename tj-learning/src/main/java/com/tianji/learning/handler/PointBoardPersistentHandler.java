package com.tianji.learning.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.contants.MySQLConstants;
import com.tianji.learning.contants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableNameContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Redis历史榜单数据持久化到数据库
 * 步骤：
 * 1、创建历史赛季榜单对应的数据分表（根据赛季ID）
 * 2、将Redis上月历史榜单数据持久化保存到数据分表中
 * 3、删除redis缓存中的上月历史榜单数据（减少内存占用）
 * 4、在XXL-JOB控制台创建以上3个任务并设置调用链顺序（指定子任务）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointBoardPersistentHandler {

    private final IPointsBoardSeasonService iPointsBoardSeasonService;

    private final IPointsBoardService iPointsBoardService;

    private final StringRedisTemplate redisTemplate;

    /**
     * 定时创建积分榜单历史分表
     * 每月执行一次，为上个月的积分赛季创建对应的历史数据分表
     */
    @XxlJob("createTableJob") // 任务名称（指定任务名称，与 XXL-JOB 管理后台配置的任务名称对应）
    public void createTableOfPointBoard(){
        LocalDateTime lastDateTime = LocalDateTime.now().minusMonths(1L);

        Integer seasonId = iPointsBoardSeasonService.getLastSeasonId(lastDateTime);
        if(Objects.isNull(seasonId)){
            return;
        }

        String tableName = StrUtil.format(MySQLConstants.POINT_BOARD_TABLENAME_PREFIX,seasonId);
        iPointsBoardService.createTableOfHistoryBoard(tableName);
    }

    /**
     * 批量持久化上月历史榜单数据到MySQL分表
     * <p>
     * 该定时任务负责将Redis中存储的上月用户积分排行榜数据分页查询并批量保存到对应的MySQL历史分表中。
     * 由于Redis有序集合中数据量可能较大，采用分页方式每次处理1000条记录，避免内存溢出。
     * </p>
     * <p>
     * 数据处理逻辑：
     * 1. 计算上月的时间并构造Redis缓存键
     * 2. 分页从Redis查询榜单数据
     * 3. 将排名(rank)转换为分表主键(id)，并清空rank和season字段
     * 4. 批量插入到MySQL历史分表
     * 5. 循环处理直到所有数据迁移完成
     * </p>
     */
    @XxlJob("PointBoardDataSaveBatch")
    public void PointBoardDataSaveBatch(){
        // 获取上月时间
        LocalDateTime lastDateTime = LocalDateTime.now().minusMonths(1L);
        String formatLastDateTime = lastDateTime.format(DateUtils.USER_POINTS_RANKING_DATE_SUFFIX_FORMATTER);
        String key = StrUtil.format(RedisConstants.USER_POINTS_RANKING_PREFIX, formatLastDateTime);

        // 将积分历史榜单分表名称保存到TreadLocal中，以便保存数据时MP动态替换表名。
        Integer seasonId = iPointsBoardSeasonService.getLastSeasonId(lastDateTime);
        TableNameContext.setInfo(StrUtil.format(MySQLConstants.POINT_BOARD_TABLENAME_PREFIX,seasonId));

        // 分页查询redis上月历史榜单数据（redis有序集合中的数据量大-服务多实例部署时Redis数据以分片方式分给不同实例执行器执行）
        int shardIndex = XxlJobHelper.getShardIndex();  //获取当前服务实例的分片编号（默认从0开始分片）
        int shardTotal = XxlJobHelper.getShardTotal();  //获取分片总数即服务实例总数

        int pageNo = shardIndex + 1;   //当前分片执行器读取数据的起始页号
        Integer pageSize = 1000;
        while (true){
            List<PointsBoard> pointsBoardList = iPointsBoardService.queryCurrentBoard(key, pageNo, pageSize);
            if(CollUtil.isEmpty(pointsBoardList)){
                log.info("当前服务分片执行器{}: 数据迁移持久化完毕！",shardIndex);
                break; //数据已经查询保存完毕直接结束
            }
            
            // 处理插入到分表的数据
            pointsBoardList.forEach(pointsBoard -> {
                pointsBoard.setId(pointsBoard.getRank().longValue())
                        .setRank(null)
                        .setSeason(null);
            });
            
            //数据批量保存到mysql(动态表明替换)
            iPointsBoardService.saveBatch(pointsBoardList);
            
            // 查询下页
            pageNo += shardTotal;  //跳过其他服务实例中的执行器保存的数据
        }

        //清理ThreadLocal中的存储的表名数据
        TableNameContext.removeInfo();
    }


    /**
     * 清理Redis中上月历史榜单数据
     * <p>
     * 该定时任务负责在历史榜单数据持久化到MySQL后，异步删除Redis中的缓存数据，以释放内存空间。
     * 使用unlink命令进行异步删除，避免阻塞Redis主线程（Redis 主线程是指 Redis 服务器中负责处理所有客户端请求的单一线程）。
     * 如果使用 del 命令，会在主线程中同步释放大量内存，阻塞其他客户端请求命令的执行。
     * </p>
     */
    @XxlJob("clearHistoryBoardDataFromRedis")
    public void clearHistoryBoardDataFromRedis(){
        // 获取上月历史榜单的缓存键
        LocalDateTime lastDateTime = LocalDateTime.now().minusMonths(1L);
        String formatLastDateTime = lastDateTime.format(DateUtils.USER_POINTS_RANKING_DATE_SUFFIX_FORMATTER);
        String key = StrUtil.format(RedisConstants.USER_POINTS_RANKING_PREFIX, formatLastDateTime);

        //异步清理redis上月历史榜单数据
        redisTemplate.unlink(key);
    }
}
