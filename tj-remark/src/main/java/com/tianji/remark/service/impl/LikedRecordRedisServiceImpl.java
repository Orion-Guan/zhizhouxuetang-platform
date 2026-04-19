package com.tianji.remark.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.Constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-07
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper rabbitMqHelper;

    private final StringRedisTemplate redisTemplate;

    /**
     * 添加或取消点赞记录
     *
     * @param likeRecordFormDTO
     */
    @Override
    public void addLikeRecord(LikeRecordFormDTO likeRecordFormDTO) {
        //判断是点赞还是取消点赞
        Boolean isSuccess = likeRecordFormDTO.getLiked() ? likes(likeRecordFormDTO) : unlikes(likeRecordFormDTO);
        if (!isSuccess) {
            log.warn("重复点赞或取消点赞");
            return;
        }

        //统计业务点赞用户数量
        String key = StrUtil.format(RedisConstants.SET_LIKED_BIZ_PREFIX, likeRecordFormDTO.getBizId());
        Long size = redisTemplate.opsForSet().size(key);
        if (null == size) {
            log.info("{}业务点赞数量为空!", key);
            return;
        }

        // 将统计的业务点赞数缓存到redis有序集合中（后续通过定时任务将其数据同步到数据库持久化）
        String bizTypeKey = StrUtil.format(RedisConstants.ZSET_LIKED_TIMES_PREFIX, likeRecordFormDTO.getBizType());
        redisTemplate.opsForZSet().add(bizTypeKey, String.valueOf(likeRecordFormDTO.getBizId()), size);
    }

    /**
     * 批量查询用户点赞状态
     *
     * @param biz
     * @return
     */
    @Override
    public Set<Long> getCLickStatusByBiz(List<Long> biz) {
        //获取用户id
        Long userId = UserContext.getUser();

        //使用redis管道批量打包发送redis命令判断用户是否点过赞（避免循环逐个执行命令性能底）
        List<Object> objects = redisTemplate.executePipelined(new RedisCallback<Boolean>() {
            /**
             * 管道批处理一次打包发送多条命令，将返回的结果一一对应封装到List集合中返回
             */
            @Override
            public Boolean doInRedis(@NotNull RedisConnection connection) throws DataAccessException {
                StringRedisConnection stringRedisConnection = (StringRedisConnection) connection;
                biz.forEach(bizId -> {
                    String key = StrUtil.format(RedisConstants.SET_LIKED_BIZ_PREFIX, bizId);
                    stringRedisConnection.sIsMember(key, String.valueOf(userId));  //不会立即执行而是将命令批量封装打包最后一起执行
                });
                return null;  // 返回值会被忽略，实际结果由 executePipelined 收集
            }
        });

        //筛选出用户点过赞的业务id
        return IntStream.range(0, biz.size())
                .filter(index -> (boolean) objects.get(index))
                .mapToObj(biz::get).collect(Collectors.toSet());
    }

    /**
     * 同步点赞数量
     * @param maxCheckCount
     */
    @Override
    public void SyncTheNumberOfLikes(String BizType, Long maxCheckCount) {
        String key = StrUtil.format(RedisConstants.ZSET_LIKED_TIMES_PREFIX, BizType);

        //读取并删除业务的点赞数（原子性--避免多实例环境下的定时任务读取相同的重复数据，执行重复同步操作）
        Set<ZSetOperations.TypedTuple<String>> tupleSet = redisTemplate.opsForZSet().popMin(key, maxCheckCount);
        if(CollUtils.isEmpty(tupleSet)){
            log.info("{}业务无要同步的点赞数",key);
            return;
        }

        //转换数据
        ArrayList<LikedTimesDTO> likedTimesDTOS = new ArrayList<>(tupleSet.size());
        for (ZSetOperations.TypedTuple<String> typedTuple : tupleSet) {
            String bizId = typedTuple.getValue();
            Double likedTimes = typedTuple.getScore();
            if(StrUtil.isBlank(bizId) || likedTimes == null){
                continue;
            }
            LikedTimesDTO likedTimesDTO = LikedTimesDTO.of(Long.valueOf(bizId), likedTimes.longValue());
            likedTimesDTOS.add(likedTimesDTO);
        }

        //发送mq消息通知相关业务持久化数据
        rabbitMqHelper.send(
                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                StrUtil.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, BizType),
                likedTimesDTOS
        );
    }

    /**
     * 取消点赞
     *
     * @param likeRecordFormDTO
     * @return
     */
    private Boolean unlikes(LikeRecordFormDTO likeRecordFormDTO) {
        // 获取用户id
        Long userId = UserContext.getUser();

        //添加点赞记录到redis中
        String key = StrUtil.format(RedisConstants.SET_LIKED_BIZ_PREFIX, likeRecordFormDTO.getBizId());
        Long removeCount = redisTemplate.opsForSet().remove(key, userId.toString());

        //返回结果
        return removeCount != null && removeCount > 0;
    }


    /**
     * 点赞
     *
     * @param likeRecordFormDTO
     * @return
     */
    private Boolean likes(LikeRecordFormDTO likeRecordFormDTO) {
        // 获取用户id
        Long userId = UserContext.getUser();

        //添加点赞记录到redis中
        String key = StrUtil.format(RedisConstants.SET_LIKED_BIZ_PREFIX, likeRecordFormDTO.getBizId());
        Long addCount = redisTemplate.opsForSet().add(key, String.valueOf(userId));

        //返回结果
        return addCount != null && addCount > 0;
    }
}
