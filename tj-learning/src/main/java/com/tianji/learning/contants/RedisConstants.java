package com.tianji.learning.contants;

public interface RedisConstants {

    /**
     * 用户签到记录Redis Key前缀
     * 格式: sign:user:{userId}:{date}
     */
    String SIGN_RECORD_PREFIX = "sign:user:{}:{}";

    /**
     * 用户积分排行榜Redis Key前缀
     */
    String USER_POINTS_RANKING_PREFIX = "board:{}";

}
