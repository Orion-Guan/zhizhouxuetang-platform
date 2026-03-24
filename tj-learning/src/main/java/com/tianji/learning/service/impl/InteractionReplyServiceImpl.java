package com.tianji.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    private final UserClient userClient;

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
        if (replyDTO.getAnswerId() == null) {
            interactionQuestionMapper.updateInfoById(replyDTO.getQuestionId(), interactionReply.getId());
        } else {
            // 评论：累计回答的回复次数
            this.lambdaUpdate().setSql("reply_times = reply_times + 1")
                    .eq(InteractionReply::getId, replyDTO.getAnswerId())
                    .update();
            // 如果回复了特定评论，同时累计该评论的回复次数
            if (replyDTO.getTargetReplyId() != null) {
                this.lambdaUpdate().setSql("reply_times = reply_times + 1")
                        .eq(InteractionReply::getId, replyDTO.getTargetReplyId())
                        .update();
            }
        }

        // 仅当提问者为学生时，将已查看的问题状态修改为未查看
        if (!replyDTO.getIsStudent()) {
            return;
        }
        InteractionQuestion question = interactionQuestionMapper.selectById(replyDTO.getQuestionId());
        if (null != question && question.getStatus() == QuestionStatus.CHECKED) {
            question.setStatus(QuestionStatus.UN_CHECK).setUpdateTime(LocalDateTime.now());
            interactionQuestionMapper.updateById(question);
        }
    }

    /**
     * 分页查询回答或评论
     *
     * @param replyPageQuery 分页查询参数对象
     *                       - questionId: 问题 ID，查询该问题下的评论时必填
     *                       - answerId: 回答 ID（评论 ID），查询该评论下的回复时填写
     *                       - pageNum: 页码
     *                       - pageSize: 每页大小
     * @return PageDTO<ReplyVO> 分页结果，包含 ReplyVO 列表和分页信息
     *         ReplyVO 包含：评论基本信息、用户信息（头像、昵称）、回复数量（一级评论）、被回复者姓名（二级回复）
     * @throws BadRequestException 当 questionId 和 answerId 同时为空时抛出
     */
    @Override
    public PageDTO<ReplyVO> queryAC(ReplyPageQuery replyPageQuery) {
        //参数效验
        if (replyPageQuery.getQuestionId() == null && replyPageQuery.getAnswerId() == null) {
            throw new BadRequestException("问题和回答id不能同时为空");
        }

        //处理回答
        Page<InteractionReply> replyPage = this.lambdaQuery()
                .eq(replyPageQuery.getQuestionId() != null, InteractionReply::getQuestionId, replyPageQuery.getQuestionId())
                .eq(replyPageQuery.getQuestionId() != null && replyPageQuery.getAnswerId() == null, InteractionReply::getAnswerId, 0L)
                .eq(replyPageQuery.getAnswerId() != null, InteractionReply::getAnswerId, replyPageQuery.getAnswerId())
                .eq(InteractionReply::getHidden, false)
                .page(replyPageQuery.toMpPage("liked_times", false));
        List<InteractionReply> replyList = replyPage.getRecords();
        if (CollUtils.isEmpty(replyList)) {
            log.info("分页查询结果为空:{}", JSON.toJSONString(replyList));
            return PageDTO.empty(replyPage);
        }

        //获取回答或评论的所属用户和评论的目标用户信息(排除匿名用户。这样自己的内容不会显示、被评论的其他人也获取个人信息)
        Set<Long> userSet = replyList.stream()
                .filter(interactionReply -> !interactionReply.getAnonymity())
                .map(InteractionReply::getUserId)
                .collect(Collectors.toSet());
        //收集目标用户id
        Set<Long> replyUserIdSet = replyList.stream()
                .map(InteractionReply::getTargetUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        userSet.addAll(replyUserIdSet);

        //收集目标回复ID
        Set<Long> longSet = replyList.stream().map(InteractionReply::getTargetReplyId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<InteractionReply> replies1 = this.lambdaQuery().in(InteractionReply::getId, longSet).list();
        Map<Long, InteractionReply> replyMap = new HashMap<>();
        if(CollUtils.isNotEmpty(replies1)){
            replyMap = replies1.stream().collect(Collectors.toMap(InteractionReply::getId, r -> r));
        }
        List<UserDTO> userDTOS = userClient.queryUserByIds(userSet);
        Map<Long, UserDTO> userDTOMap;
        if (CollUtils.isNotEmpty(userDTOS)) {
            userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        } else {
            userDTOMap = null;
        }

        //处理回答或评论下的评论显示的数量
        Set<Long> answerIdSet = replyList.stream().map(InteractionReply::getId).collect(Collectors.toSet());
        Map<Long, Long> longLongMap = new HashMap<>();
        if (replyPageQuery.getAnswerId() == null) {
            List<InteractionReply> replies = this.lambdaQuery()
                    .in(InteractionReply::getAnswerId, answerIdSet)
                    .eq(InteractionReply::getHidden, false)
                    .list();
            if(CollUtils.isNotEmpty(replies)){
                longLongMap = replies.stream().collect(Collectors.groupingBy(InteractionReply::getAnswerId, Collectors.counting()));
            }
        }

        //封装返回数据
        Map<Long, Long> finalLongLongMap = longLongMap;
        Map<Long, InteractionReply> finalReplyMap = replyMap;
        List<ReplyVO> replyVOList = replyList.stream().map(interactionReply -> {
            ReplyVO replyVO = BeanUtils.copyBean(interactionReply, ReplyVO.class);
            if (!interactionReply.getHidden() && CollUtils.isNotEmpty(userDTOMap) && interactionReply.getUserId() != null) {
                UserDTO userDTO = userDTOMap.getOrDefault(interactionReply.getUserId(), null);
                replyVO.setUserIcon(userDTO != null ? userDTO.getIcon() : null);
                replyVO.setUserName(userDTO != null ? userDTO.getName() : null);
            }
            //只有被评论的用户为非匿名时，此评论才显示目标用户名称
            if (replyPageQuery.getAnswerId() != null && CollUtils.isNotEmpty(userDTOMap)) {
                UserDTO userDTO = userDTOMap.getOrDefault(interactionReply.getTargetUserId(), null);
                if(CollUtils.isNotEmpty(finalReplyMap) && finalReplyMap.get(interactionReply.getTargetReplyId()) != null){
                    InteractionReply interactionReply1 = finalReplyMap.get(interactionReply.getTargetReplyId());
                    if(!interactionReply1.getAnonymity()){
                        replyVO.setTargetUserName(userDTO.getName());
                    }
                }
            }
            if (replyPageQuery.getAnswerId() == null && CollUtils.isNotEmpty(finalLongLongMap)) {
                Long aLong = finalLongLongMap.get(interactionReply.getId());
                replyVO.setReplyTimes(Math.toIntExact(aLong != null? aLong:0L));
            }
            return replyVO;
        }).collect(Collectors.toList());
        return PageDTO.of(replyPage, replyVOList);
    }
}
