package com.core.coreapi.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * 接口文档
 */
@Document(indexName = "api_doc")
@Data
public class ApiDoc implements Serializable {

    /**
     * 主键
     */
    @Id
    private Long id;

    /**
     * 创建人 id
     */
    private Long userId;

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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除(0-未删,1-已删)
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;

}