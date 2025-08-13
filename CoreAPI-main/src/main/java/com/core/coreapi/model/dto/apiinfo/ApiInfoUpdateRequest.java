package com.core.coreapi.model.dto.apiinfo;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新请求
 *
 */
@Data
public class ApiInfoUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

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
     */
    private String queryExample;

    /**
     * 请求体示例
     */
    private String reqBodyExample;

    private static final long serialVersionUID = 1L;

}