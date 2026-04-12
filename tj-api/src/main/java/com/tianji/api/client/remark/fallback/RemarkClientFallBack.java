package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.Set;

@Slf4j
public class RemarkClientFallBack implements FallbackFactory<RemarkClient> {

    @Override
    public RemarkClient create(Throwable cause) {

        log.error("remark-service服务远程调用异常:",cause);

        return new RemarkClient() {
            @Override
            public Set<Long> getCLickStatusByBiz(Iterable<Long> biz) {
                return Collections.emptySet();
            }
        };
    }
}
