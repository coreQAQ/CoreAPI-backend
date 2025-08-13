package com.core.coreapi.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "数据校验请求")
public class ValidateRequest implements Serializable {

    @Schema(description = "待校验的字符串列表", example = "[\"coreqaq@gmail.com\", \"invalid_email\"]")
    private List<String> values;

    @Schema(
            description = "校验类型。可选值：" +
                    "email(邮箱), mobile(手机号), chinese(中文), idcard(身份证号), url(URL), " +
                    "mac(MAC地址), ipv4(IPv4地址), ipv6(IPv6地址), zipcode(邮政编码), " +
                    "birthday(生日), plate(车牌号)",
            example = "email"
    )
    private String type;

    private static final long serialVersionUID = 1L;

}