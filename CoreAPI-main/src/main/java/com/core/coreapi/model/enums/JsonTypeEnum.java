package com.core.coreapi.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * json类型枚举
 *
 */
@Getter
public enum JsonTypeEnum {

    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean"),
    OBJECT("object");

    private final String value;

    JsonTypeEnum(String value) {
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
