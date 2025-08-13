package com.core.coreapi.model.dto.doc;

import lombok.Data;

@Data
public class GenDocRequest {

    /**
     * OpenAPI 地址
     */
    private String url;

    /**
     * 接口名称
     */
    private String name;

    /**
     * 接口路径
     */
    private String path;

    /**
     * 接口方法
     */
    private String method;

}
