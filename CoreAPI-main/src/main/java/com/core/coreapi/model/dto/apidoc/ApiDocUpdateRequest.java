package com.core.coreapi.model.dto.apidoc;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新接口文档请求
 *
 */
@Data
public class ApiDocUpdateRequest implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档描述
     */
    private String description;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 优先级
     */
    private Integer priority;

    private static final long serialVersionUID = 1L;

}