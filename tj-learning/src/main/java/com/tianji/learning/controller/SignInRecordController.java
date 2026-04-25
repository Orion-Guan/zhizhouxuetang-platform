package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignInRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("sign-records")
@RestController
@RequiredArgsConstructor
@Slf4j
@Api(tags = "学员签到相关接口")
public class SignInRecordController {

    private final ISignInRecordService iSignInRecordService;


    /**
     * 添加签到记录
     * @return
     */
    @PostMapping
    @ApiOperation("添加签到记录")
    public SignResultVO addSignRecord(){
        log.debug("添加签到记录开始！");
        return iSignInRecordService.addSignRecord();
    }

    /**
     * 查询当月签到记录
     * @return 当月第一天到今日的签到记录，0表示未签到，1表示已签到
     */
    @GetMapping
    @ApiOperation("查询当月签到记录")
    public List<Integer> getSignRecords(){
        return iSignInRecordService.getSignRecords();
    }


}
