package com.core.coreapi.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 参数位置枚举
 *
 */
@Getter
public enum PositionEnum {

    PATH("PATH"),
    QUERY("QUERY"),
    BODY("BODY"),
    HEADER("HEADER");

    private final String value;

    PositionEnum(String value) {
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

}