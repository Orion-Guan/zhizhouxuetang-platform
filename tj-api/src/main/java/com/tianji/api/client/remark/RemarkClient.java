package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@FeignClient(value = "remark-service",fallbackFactory = RemarkClientFallBack.class)
public interface RemarkClient {

    /**
     * 批量查询用户指定业务点赞状态
     * @param biz
     * @return
     */
    @GetMapping("/likes/list")
    Set<Long> getCLickStatusByBiz(@RequestParam("biz") Iterable<Long> biz);
}
