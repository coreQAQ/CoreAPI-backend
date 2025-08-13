package com.core.coreapi.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 必选枚举
 *
 */
@Getter
public enum RequireEnum {

    REQUIRED(1),
    NOT_REQUIRED(0);

    private final Integer value;

    RequireEnum(Integer value) {
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

}
