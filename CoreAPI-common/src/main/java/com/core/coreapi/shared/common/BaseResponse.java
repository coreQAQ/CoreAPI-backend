package com.core.coreapi.shared.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T> 返回数据类型
 */
@Schema(description = "通用响应")
@Data
public class BaseResponse<T> implements Serializable {

    @Schema(description = "业务状态码", example = "0")
    private int code;

    @Schema(description = "数据")
    private T data;

    @Schema(description = "响应消息", example = "ok")
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
