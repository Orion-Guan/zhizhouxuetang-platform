package com.tianji.learning.controller;


import cn.hutool.json.JSONUtil;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author Orion.Guan
 * @since 2026-03-20
 */
@Slf4j
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
@Api(tags = "问题相关接口")
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    /**
     * 新增用户问题
     * @param questionFormDTO
     */
    @PostMapping
    @ApiOperation("用户新增问题")
    private void saveQuestion(@RequestBody @Valid  QuestionFormDTO questionFormDTO){
        log.debug("新增问题入参:{}", JSONUtil.toJsonStr(questionFormDTO));
        questionService.saveQuestion(questionFormDTO);
    }


    /**
     * 用户端分页查询问题
     * @param query
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("用户端分页查询问题")
    public PageDTO<QuestionVO> queryQuestionsForPage(@Valid QuestionPageQuery query){
        log.debug("用户端分页查询接口入参:{}",JSONUtil.toJsonStr(query));
        return questionService.queryQuestionsForPage(query);
    }

    /**
     * 根据Id查询问题详情
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @ApiOperation("根据Id查询问题详情")
    public QuestionVO queryQuestionById(@PathVariable("id") Long id){
        log.debug("查询问题详情入参:{}",id);
        return questionService.queryQuestionById(id);
    }

}
