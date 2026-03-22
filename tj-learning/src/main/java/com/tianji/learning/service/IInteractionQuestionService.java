package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-20
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    /**
     * 用户新增问题
     * @param questionService
     */
    void saveQuestion(QuestionFormDTO questionService);

    /**
     * 分页查询用户问题
     * @param query
     * @return
     */
    PageDTO<QuestionVO> queryQuestionsForPage(QuestionPageQuery query);

    /**
     * 查询问题详情
     * @param id
     * @return
     */
    QuestionVO queryQuestionById(Long id);

    /**
     * 管理端分页查询问题
     * @param query
     * @return
     */
    PageDTO<QuestionAdminVO> queryAdminQuestionsForPage(QuestionAdminPageQuery query);

    /**
     * 管理端查询问题详情
     * @param id
     * @return
     */
    QuestionAdminVO queryQuestionAdminById(Long id);

}
