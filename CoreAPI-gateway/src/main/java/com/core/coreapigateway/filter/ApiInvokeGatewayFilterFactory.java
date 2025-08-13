package com.core.coreapigateway.filter;

import cn.hutool.core.util.RandomUtil;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.entity.ApiInfo;
import com.core.coreapi.shared.entity.User;
import com.core.coreapi.shared.enums.ApiStatusEnum;
import com.core.coreapi.shared.service.InnerApiInfoService;
import com.core.coreapi.shared.service.InnerUserService;
import com.core.coreapi.shared.utils.SignUtil;
import com.core.coreapigateway.constant.RedisKey;
import com.core.coreapigateway.constant.ScriptConstant;
import com.core.coreapigateway.entity.ApiAccessLog;
import com.core.coreapigateway.exception.BusinessException;
import com.core.coreapigateway.producer.ApiLogProducer;
import com.core.coreapigateway.service.ElasticsearchService;
import com.core.coreapigateway.utils.NetUtils;
import com.core.coreapigateway.utils.RedisUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 调用接口过滤器
 * 限流 ==》 身份校验 ==》 接口状态校验
 * 调用结果发送到kafka 《==
 */
@Component
public class ApiInvokeGatewayFilterFactory extends AbstractGatewayFilterFactory<ApiInvokeGatewayFilterFactory.Config> {

    @DubboReference
    private InnerUserService userService;

    @DubboReference
    private InnerApiInfoService apiInfoService;

    @Resource
    private ElasticsearchService elasticsearchService;

    @Resource
    private RedissonClient client;

    @Resource
    private ApiLogProducer producer;

    @Data
    public static class Config {
        private long windowMillis = Duration.ofMinutes(1).toMillis(); // 窗口大小
        private int limit = 100; // 请
    }

    public ApiInvokeGatewayFilterFactory() {
        super(Config.class); // 绑定配置类
    }

    @Override
    public GatewayFilter apply(Config config) {
        // 实现 GatewayFilter 接口的 apply 方法
        return ((exchange, chain) -> {
            return authCheck(exchange, chain, config);
        });
    }

    /**
     * 身份认证
     *
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> authCheck(ServerWebExchange exchange, GatewayFilterChain chain, Config config) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        // 1.校验
        final String accessKey = headers.getFirst("accessKey");
        final String nonce = headers.getFirst("nonce");
        final String timestamp = headers.getFirst("timestamp");
        final String sign = headers.getFirst("sign");
        if (StringUtils.isAnyBlank(accessKey, nonce, timestamp, sign)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求头不完整");
        }

        // 签名一分钟内有效防止重放XHR
        if ((System.currentTimeMillis() - Long.parseLong(timestamp)) >= Duration.ofMinutes(1).toMillis()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "会话已过期");
        }
        RBucket<String> bucket = client.getBucket(String.format(RedisKey.NONCE, nonce));
        if (!bucket.setIfAbsent(nonce, Duration.ofMinutes(1))) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求重复");
        }

        // 限流
        handleRateLimit(accessKey, config);

        // 根据accessKey获取用户信息
        User user = userService.getUserByAccessKey(accessKey);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "accessKey不存在");
        }

        // 校验签名
        String secretKey = user.getSecretKey();
        String realSign = SignUtil.genSign(timestamp, secretKey);
        if (!sign.equals(realSign)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "签名错误");
        }
        // 获取接口信息
        String path = request.getPath().value();
        String method = request.getMethodValue();
        ApiInfo apiInfo = apiInfoService.getApiInfo(path, method);
        if (apiInfo == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "接口不存在");
        }
        // 接口状态校验 非管理员或接口未上线时禁止访问
        Integer status = apiInfo.getStatus();
        if (ApiStatusEnum.OFFLINE.getValue().equals(status) && !"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "接口未上线");
        }
        return handleResponse(exchange, chain, user, apiInfo);
    }

    /**
     * 记录调用结果
     *
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, User user, ApiInfo apiInfo) {
        // 最终调用结果写入es
        return chain.filter(exchange).doFinally(signalType -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

            ApiAccessLog apiAccessLog = new ApiAccessLog();
            apiAccessLog.setAk(user.getAccessKey());
            apiAccessLog.setUri(request.getURI().getPath());
            apiAccessLog.setMethod(request.getMethodValue());
            apiAccessLog.setStatusCode(statusCode);
            Long startTime = exchange.getAttribute(LogFilter.START_TIME);
            apiAccessLog.setDuration(System.currentTimeMillis() - startTime);
            apiAccessLog.setIp(NetUtils.getIpAddress(request));
            apiAccessLog.setUserAgent(request.getHeaders().getFirst(HttpHeaders.USER_AGENT));
            apiAccessLog.setTimestamp(Instant.now().toString());

            producer.send(apiAccessLog);
        });
    }

    private void handleRateLimit(String accessKey, Config config) {
        // redis 滑窗限流
        List<Object> keys = Collections.singletonList(RedisKey.RATE_LIMIT + accessKey);
        // member 改为时间戳+随机三位数 防止并发时记录重复
        List<Object> args = Arrays.asList(System.currentTimeMillis(), config.getWindowMillis(), config.getLimit());
        RScript script = client.getScript(StringCodec.INSTANCE);
        Boolean result = script.eval(
                RScript.Mode.READ_WRITE,
                RedisUtils.loadScript(ScriptConstant.RATE_LIMIT),
                RScript.ReturnType.BOOLEAN,
                keys,
                args.toArray()
        );

        // 执行限流
        if (!result) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "访问频率过高");
        }

    }

}
