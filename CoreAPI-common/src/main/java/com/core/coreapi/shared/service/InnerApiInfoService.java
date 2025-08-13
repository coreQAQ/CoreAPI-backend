package com.core.coreapi.shared.service;

import com.core.coreapi.shared.entity.ApiInfo;

/**
 * 内部接口信息乳
 *
 */
public interface InnerApiInfoService {

    public ApiInfo getApiInfo(String path, String method);

}