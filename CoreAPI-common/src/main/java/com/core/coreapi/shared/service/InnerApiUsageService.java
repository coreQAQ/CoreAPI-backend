package com.core.coreapi.shared.service;

import com.core.coreapi.shared.entity.ApiUsage;

import java.util.List;

/**
 * 内部调用统计服务
 *
 */
public interface InnerApiUsageService {

    /**
     * 批量更新用户调用次数
     *
     * @param apiUsageList
     */
    public void batchRecordApiUsage(List<ApiUsage> apiUsageList);

}
