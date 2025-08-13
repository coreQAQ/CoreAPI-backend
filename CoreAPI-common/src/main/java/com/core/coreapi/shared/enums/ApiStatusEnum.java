package com.core.coreapi.shared.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 接口状态枚举
 *
 */
@Getter
public enum ApiStatusEnum {

    OFFLINE(0),
    ONLINE(1);

    private final Integer value;

    ApiStatusEnum(Integer value) {
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
