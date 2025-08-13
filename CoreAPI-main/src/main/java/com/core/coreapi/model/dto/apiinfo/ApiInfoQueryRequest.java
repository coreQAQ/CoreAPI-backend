package com.core.coreapi.model.dto.apiinfo;

import com.core.coreapi.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiInfoQueryRequest extends PageRequest implements Serializable {

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

    private static final long serialVersionUID = 1L;

}