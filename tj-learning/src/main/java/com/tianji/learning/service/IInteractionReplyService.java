package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;

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

    /**
     * 查询互动的回答或评论
     * @param replyPageQuery
     * @return
     */
    PageDTO<ReplyVO> queryAC(ReplyPageQuery replyPageQuery);
}
