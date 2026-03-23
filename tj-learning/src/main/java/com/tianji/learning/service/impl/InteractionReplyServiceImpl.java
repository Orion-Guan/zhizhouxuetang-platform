package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper interactionQuestionMapper;

    /**
     * 保存回答或评论，并更新相关统计信息
     * 功能包括：保存回复数据、更新问题的最新回答 ID 和 回答次数、累计回答或评论的回复次数、修改问题查看状态
     *
     * @param replyDTO 回复数据传输对象，包含问题 ID、答案 ID、目标回复 ID、是否学生等信息
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAC(ReplyDTO replyDTO) {
        // 将 DTO 转换为实体对象并设置当前用户 ID 后保存到数据库
        InteractionReply interactionReply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        interactionReply.setUserId(UserContext.getUser());
        this.save(interactionReply);

        // 判断是回答还是评论：回答则更新问题的最新答案 ID 和答题次数
        if(replyDTO.getAnswerId() == null){
            interactionQuestionMapper.updateInfoById(replyDTO.getQuestionId(), interactionReply.getId());
        }else {
            // 评论：累计回答的回复次数
            this.lambdaUpdate().setSql("reply_times = reply_times + 1")
                    .eq(InteractionReply::getId, replyDTO.getAnswerId())
                    .update();
            // 如果回复了特定评论，同时累计该评论的回复次数
            if(replyDTO.getTargetReplyId() != null){
                this.lambdaUpdate().setSql("reply_times = reply_times + 1")
                        .eq(InteractionReply::getId, replyDTO.getTargetReplyId())
                        .update();
            }
        }

        // 仅当提问者为学生时，将已查看的问题状态修改为未查看
        if(!replyDTO.getIsStudent()){
            return;
        }
        InteractionQuestion question = interactionQuestionMapper.selectById(replyDTO.getQuestionId());
        if(null != question && question.getStatus() == QuestionStatus.CHECKED){
            question.setStatus(QuestionStatus.UN_CHECK).setUpdateTime(LocalDateTime.now());
            interactionQuestionMapper.updateById(question);
        }
    }
}
