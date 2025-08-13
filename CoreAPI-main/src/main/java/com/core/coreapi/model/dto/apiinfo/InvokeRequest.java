package com.core.coreapi.model.dto.apiinfo;

import lombok.Data;

import java.io.Serializable;

@Data
public class InvokeRequest implements Serializable {

    /**
     * 接口id
     */
    private Long id;

    /**
     * 查询参数
     */
    private String query;

    /**
     * 请求体
     */
    private String body;

    private static final long serialVersionUID = 1L;

}