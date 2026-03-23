package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.po.InteractionQuestion;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 互动提问的问题表 Mapper 接口
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-20
 */
public interface InteractionQuestionMapper extends BaseMapper<InteractionQuestion> {

    @Update("update interaction_question set latest_answer_id = #{id}, answer_times = answer_times+1 where id = #{questionId}")
    Integer updateInfoById(Long questionId, Long id);
}
