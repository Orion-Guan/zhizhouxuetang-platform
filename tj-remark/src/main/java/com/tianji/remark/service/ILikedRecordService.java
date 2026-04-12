package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-04-07
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    /**
     * 添加或取消点赞记录
     * @param likeRecordFormDTO
     */
    void addLikeRecord(@Valid LikeRecordFormDTO likeRecordFormDTO);


    /**
     * 获取点赞状态
     * @return
     */
    Set<Long> getCLickStatusByBiz(List<Long> biz);
}
