package com.tianji.learning.controller;


import cn.hutool.json.JSONUtil;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
@Api(tags = "问题相关接口")
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService questionService;



    /**
     * 管理端分页查询问题
     */
    @GetMapping("page")
    @ApiOperation("管理端分页查询问题")
    public PageDTO<QuestionAdminVO> queryAdminQuestionsForPage(QuestionAdminPageQuery query){
        log.debug("管理端分页查询接口入参:{}",JSONUtil.toJsonStr(query));
        return questionService.queryAdminQuestionsForPage(query);
    }


    /**
     * 管理端根据问题ID查询其详情信息
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @ApiOperation("管理端根据问题ID查询其详情信息")
    public QuestionAdminVO queryQuestionAdminById(@PathVariable("id") Long id){
        log.debug("管理端查询问题详情入参:{}",id);
        return questionService.queryQuestionAdminById(id);
    }



}
