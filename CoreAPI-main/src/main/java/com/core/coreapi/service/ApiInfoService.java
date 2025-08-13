package com.core.coreapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.core.coreapi.model.dto.apiinfo.ApiInfoQueryRequest;
import com.core.coreapi.model.vo.ApiInfoVO;
import com.core.coreapi.shared.entity.ApiInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 接口信息服务
 *
*/
public interface ApiInfoService extends IService<ApiInfo> {

    /**
     * 校验
     *
     * @param apiInfo
     * @param add
     */
    void validApiInfo(ApiInfo apiInfo, boolean add);

    /**
     * 获取查询条件
     *
     * @param apiInfoQueryRequest
     * @return
     */
    //
    QueryWrapper<ApiInfo> getQueryWrapper(ApiInfoQueryRequest apiInfoQueryRequest);

    /**
     * 获取封装
     *
     * @param apiInfo
     * @return
     */
    ApiInfoVO getApiInfoVO(ApiInfo apiInfo);

    /**
     * 获取封装列表
     *
     * @param apiInfoList
     * @return
     */
    List<ApiInfoVO> getApiInfoVOList(List<ApiInfo> apiInfoList);

    /**
     * 分页获取封装
     *
     * @param apiInfoPage
     * @return
     */
    Page<ApiInfoVO> getApiInfoVOPage(Page<ApiInfo> apiInfoPage);

    /**
     * 调用接口，返回响应体
     *
     * @param url
     * @param method
     * @param query
     * @param body
     * @param request
     * @return
     */
    String invokeApi(String url, String method, String query, String body, HttpServletRequest request);

}
