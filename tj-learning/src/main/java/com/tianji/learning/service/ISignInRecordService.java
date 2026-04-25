package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;

import java.util.List;

public interface ISignInRecordService {

    /**
     * 学员签到
     * @return
     */
    SignResultVO addSignRecord();

    /**
     * 查询当月签到记录
     * @return 当月第一天到今日的签到记录，0表示未签到，1表示已签到
     */
    List<Integer> getSignRecords();
}
