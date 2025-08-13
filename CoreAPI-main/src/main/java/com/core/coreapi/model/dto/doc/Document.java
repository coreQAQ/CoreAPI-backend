package com.core.coreapi.model.dto.doc;

import lombok.Data;

import java.util.List;

/**
 * 文档数据封装类
 */
@Data
public class Document {

    /**
     * 接口名
     */
    private String name;

    /**
     * 接口路径
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求参数
     */
    private List<Param> reqParams;

    /**
     * 响应参数
     * 无position, required, example等字段
     */
    private List<Param> respParams;

    /**
     * 查询参数示例
     * 例如：userId=123&status=active
     */
    private String queryExample;

    /**
     * 请求体示例
     */
    private String reqBodyExample;

    /**
     * 响应示例
     */
    private String respExample;

    /**
     * 文档内容
     */
    private String content;

}