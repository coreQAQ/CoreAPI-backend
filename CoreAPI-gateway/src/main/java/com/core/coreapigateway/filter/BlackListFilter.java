package com.core.coreapigateway.filter;

import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapigateway.constant.RedisKey;
import com.core.coreapigateway.exception.BusinessException;
import com.core.coreapigateway.utils.NetUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 黑名单过滤器
 */
@Component
public class BlackListFilter implements GlobalFilter, Ordered {

    @Resource
    private RedissonClient client;

    @Resource
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取客户端 IP 地址
        ServerHttpRequest request = exchange.getRequest();
        String ip = NetUtils.getIpAddress(request);
        // 2.获取黑名单列表并检测
        RSet<String> set = client.getSet(RedisKey.BLACKLIST);
        boolean isBlack = set.readAll().stream()
                .anyMatch(blackIp -> ip.equals(blackIp));
        if (isBlack) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "IP 在黑名单中，禁止访问");
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
