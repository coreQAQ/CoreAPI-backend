package com.core.coreapigateway.exception;

import com.core.coreapi.shared.common.BaseResponse;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.common.ResultUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 设置响应状态码和头信息
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 自定义返回的异常信息
        BaseResponse<?> response;
        if (ex instanceof BusinessException) {
            BusinessException businessException = (BusinessException) ex;
            log.error("BusinessException: ", businessException);
            response = ResultUtils.error(businessException.getCode(), businessException.getMessage());
        } else if (ex instanceof RuntimeException) {
            log.error("RuntimeException: ", ex);
            response = ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
        } else {
            log.error("Unexpected Exception: ", ex);
            response = ResultUtils.error(ErrorCode.SYSTEM_ERROR, "未知错误");
        }

        // 将响应序列化为 JSON
        String jsonResponse = serializeResponse(response);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory().
                wrap(jsonResponse.getBytes());

        // 写入响应并完成处理
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String serializeResponse(BaseResponse<?> response) {
        try {
            // 使用 Jackson 或其他 JSON 序列化工具
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to serialize response", e);
            return "{\"code\":500,\"message\":\"系统错误\"}";
        }
    }
}
