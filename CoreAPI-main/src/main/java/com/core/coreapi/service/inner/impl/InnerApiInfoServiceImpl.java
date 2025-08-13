package com.core.coreapi.service.inner.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.core.coreapi.constant.RedisKey;
import com.core.coreapi.mapper.ApiInfoMapper;
import com.core.coreapi.shared.entity.ApiInfo;
import com.core.coreapi.shared.service.InnerApiInfoService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.annotation.Resource;

/**
 * 内部接口信息服务
 */
@DubboService
public class InnerApiInfoServiceImpl implements InnerApiInfoService {

    @Resource
    private ApiInfoMapper apiInfoMapper;

    @Override
    public ApiInfo getApiInfo(@Nullable String path, @Nullable String method) {
        if (StringUtils.isAnyBlank(path, method)) return null;
        InnerApiInfoServiceImpl innerApiInfoService = (InnerApiInfoServiceImpl) AopContext.currentProxy();
        Long id = innerApiInfoService.getIdByPM(path, method);
        if (id == null || id <= 0) return null;
        ApiInfo apiInfo = innerApiInfoService.getApiInfoById(id);
        // 防止数据不一致
        if (!path.equals(apiInfo.getPath()) || !method.equals(apiInfo.getMethod())) return null;

        return apiInfo;
    }

    @Cacheable(
            cacheNames = RedisKey.SC_PM2ID,
            key = "#path + ':' +  #method",
            sync = true
    )
    public Long getIdByPM(@NonNull String path, @NonNull String method) {
        ApiInfo apiInfo = apiInfoMapper.selectOne(new QueryWrapper<ApiInfo>()
                .eq("path", path)
                .eq("method", method.toUpperCase())
                .select("id"));
        return apiInfo == null ? null : apiInfo.getId();
    }

    @Cacheable(
            cacheNames = RedisKey.SC_API_INFO,
            key = "#id",
            sync = true
    )
    public ApiInfo getApiInfoById(@NonNull Long id) {
        return apiInfoMapper.selectById(id);
    }

}
