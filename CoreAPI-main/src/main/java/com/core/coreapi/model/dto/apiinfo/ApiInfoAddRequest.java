package com.core.coreapi.model.dto.apiinfo;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加请求
 *
 */
@Data
public class ApiInfoAddRequest implements Serializable {

    /**
     * 文档id
     */
    private Long docId;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * OpenAPI 文档地址
     * 可选
     */
    private String openapiUrl;

    /**
     * 主机
     */
    private String host;

    /**
     * 端口
     */
    private String port;

    /**
     * 路径
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 每次调用花费
     */
    private Integer cost;

    /**
     * 查询参数示例，例: key1=value1&key2=value2
     * 可选
     */
    private String queryExample;

    /**
     * 请求体示例
     * 可选
     */
    private String reqBodyExample;

    private static final long serialVersionUID = 1L;

}