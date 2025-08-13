package com.core.coreapi.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreapi.model.dto.apidoc.ApiDocQueryRequest;
import com.core.coreapi.model.dto.doc.Document;
import com.core.coreapi.model.entity.ApiDoc;

import java.util.List;

/**
 * 接口文档服务
 */
public interface ApiDocService {

    /**
     * 校验数据
     *
     * @param apiDoc
     * @param add    对创建的数据进行校验
     */
    void validApiDoc(ApiDoc apiDoc, boolean add);

    /**
     * 查询接口文档
     *
     * @param id
     * @return
     */
    ApiDoc getById(Long id);

    /**
     * 添加接口文档
     *
     * @param apiDoc
     * @return
     */
    Long add(ApiDoc apiDoc);

    /**
     * 更新接口文档
     *
     * @param apiDoc
     * @return
     */
    boolean updateById(ApiDoc apiDoc);

    /**
     * 逻辑删除接口文档
     *
     * @param id
     */
    boolean deleteById(Long id);

    /**
     * 获取接口文档概要列表
     *
     * @return
     */
    List<ApiDoc> listApiDocBrief();

    /**
     * 生成接口文档
     *
     * @param url openapiUrl
     * @param name 接口名
     * @param path 接口路径
     * @param method 接口请求方法
     * @return
     */
    Document genDoc(String url, String name, String path, String method);

    /**
     * 查询
     *
     * @param apiDocQueryRequest
     * @return
     */
    Page<ApiDoc> searchFromEs(ApiDocQueryRequest apiDocQueryRequest);
}
