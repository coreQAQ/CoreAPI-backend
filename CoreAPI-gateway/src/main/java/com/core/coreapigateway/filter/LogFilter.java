package com.core.coreapigateway.filter;

import com.core.coreapigateway.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class LogFilter implements GlobalFilter, Ordered {

    public static final String START_TIME = "start_time";
    public static final String REQ_ID = "request_id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 记录开始时间和唯一id
        Map<String, Object> attributes = exchange.getAttributes();
        long startTime = System.currentTimeMillis();
        attributes.put(START_TIME, startTime);

        String requestId = UUID.randomUUID().toString();
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQ_ID, requestId)
                .build();

        String url = request.getURI().toString();
        log.info("request start，id: {}, path: {}, ip: {}", requestId, url,
                NetUtils.getIpAddress(request));

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange)
                .doFinally((signal) -> {
                    log.info("request end, id: {}, cost: {}ms", requestId, System.currentTimeMillis() - startTime);
                });
    }

    @Override
    public int getOrder() {
        return -9;
    }
}
