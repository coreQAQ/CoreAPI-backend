package com.core.coreapi.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "校验结果")
public class ValidateResult implements Serializable {
    @Schema(description = "原始字符串列表", example = "[\"coreqaq@gmail.com\", \"invalid_email\"]")
    private List<String> values;

    @Schema(description = "校验类型", example = "email")
    private String type;

    @Schema(description = "每一项是否校验通过", example = "[true, false]")
    private List<Boolean> isValid;

    private static final long serialVersionUID = 1L;
}