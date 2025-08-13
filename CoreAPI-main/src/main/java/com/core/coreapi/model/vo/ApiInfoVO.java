package com.core.coreapi.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 接口信息视图
 *
 */
@Data
public class ApiInfoVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 创建人
     */
    private Long userId;

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
     * 接口状态(0-关闭,1-开启)
     */
    private Integer status;

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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;

}