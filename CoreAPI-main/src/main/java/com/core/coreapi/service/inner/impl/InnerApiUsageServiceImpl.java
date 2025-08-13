package com.core.coreapi.service.inner.impl;

import com.core.coreapi.mapper.ApiUsageMapper;
import com.core.coreapi.shared.entity.ApiUsage;
import com.core.coreapi.shared.service.InnerApiUsageService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 内部调用统计服务实现
 */
@DubboService
public class InnerApiUsageServiceImpl implements InnerApiUsageService {

    @Resource
    private ApiUsageMapper apiUsageMapper;

    @Override
    public void batchRecordApiUsage(List<ApiUsage> apiUsageList) {

    }

}
