package com.tianji.learning.service;

import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-20
 */
public interface IInteractionReplyService extends IService<InteractionReply> {

    /**
     * 新增问题的回答或评论
     * @param replyDTO
     */
    void saveAC(ReplyDTO replyDTO);
}
