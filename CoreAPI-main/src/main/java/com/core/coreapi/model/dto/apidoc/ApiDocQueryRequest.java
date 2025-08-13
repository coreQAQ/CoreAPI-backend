package com.core.coreapi.model.dto.apidoc;

import com.core.coreapi.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 查询接口文档请求
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApiDocQueryRequest extends PageRequest implements Serializable {

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
     * 查询内容
     */
    private String searchText;

    private static final long serialVersionUID = 1L;

}