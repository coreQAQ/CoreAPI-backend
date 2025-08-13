package com.core.coreapi.model.dto.doc;

import lombok.Data;

import java.util.List;

@Data
public class Param {

    /**
     * 字段名，如 "user", "address.city"
     */
    private String name;

    /**
     * 字段类型，如 string、integer、object、array
     */
    private String type;

    /**
     * 参数位置
     */
    private String position;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 字段描述，如 "用户姓名"
     */
    private String description;

    /**
     * 示例值，例如 "张三"
     */
    private Object example;

    /**
     * 嵌套字段（仅当 type == "object" 或 type == "array<object>" 时存在）
     */
    private List<Param> children;



}
