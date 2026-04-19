package com.tianji.remark.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-07
 */
//@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper rabbitMqHelper;

    /**
     * 添加或取消点赞记录
     * @param likeRecordFormDTO
     */
    @Override
    public void addLikeRecord(LikeRecordFormDTO likeRecordFormDTO) {
        //判断是点赞还是取消点赞
        Boolean isSuccess = likeRecordFormDTO.getLiked() ? likes(likeRecordFormDTO):unlikes(likeRecordFormDTO);
        if (!isSuccess) {
            log.warn("重复点赞或取消点赞");
            return;
        }

        //统计业务点赞用户数量
        Long count = this.lambdaQuery().eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId()).count();

        //高并发更新操作使用基于MQ的异步消息通知
        rabbitMqHelper.send(MqConstants.Exchange.LIKE_RECORD_EXCHANGE, StrUtil.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, likeRecordFormDTO.getBizType()), LikedTimesDTO.of(likeRecordFormDTO.getBizId(), count));
    }

    /**
     * 批量查询用户点赞状态
     * @param biz
     * @return
     */
    @Override
    public Set<Long> getCLickStatusByBiz(List<Long> biz) {
        Long userId = UserContext.getUser();
        List<LikedRecord> list = this.lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .in(LikedRecord::getBizId, biz)
                .list();
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }


    @Override
    public void SyncTheNumberOfLikes(String key, Long maxCheckCount) {

    }

    /**
     * 取消点赞
     * @param likeRecordFormDTO
     * @return
     */
    private Boolean unlikes(LikeRecordFormDTO likeRecordFormDTO) {
        LambdaQueryWrapper<LikedRecord> queryWrapper = new LambdaQueryWrapper<LikedRecord>()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId());
        return this.remove(queryWrapper);
    }


    /**
     * 点赞
     * @param likeRecordFormDTO
     * @return
     */
    private Boolean likes(LikeRecordFormDTO likeRecordFormDTO) {
        //判断是否重复点赞
        boolean isExist = this.lambdaQuery().eq(LikedRecord::getUserId, UserContext.getUser()).eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId()).exists();
        if (isExist) {
            return false;
        }

        //新增点赞记录
        LikedRecord likedRecord = new LikedRecord();
        likedRecord.setUserId(UserContext.getUser()).setBizId(likeRecordFormDTO.getBizId()).setBizType(likeRecordFormDTO.getBizType()).setUpdateTime(LocalDateTime.now());
        return this.save(likedRecord);
    }
}
