package com.core.coreapi.service.inner.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.core.coreapi.constant.RedisKey;
import com.core.coreapi.mapper.UserMapper;
import com.core.coreapi.shared.entity.User;
import com.core.coreapi.shared.service.InnerUserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.annotation.Resource;

/**
 * 内部用户服务
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedissonClient client;

    @Override
    public User getUserByAccessKey(@Nullable String accessKey) {
        if (StringUtils.isAnyBlank(accessKey)) return null;
        InnerUserServiceImpl innerUserService = (InnerUserServiceImpl) AopContext.currentProxy();
        Long id = innerUserService.getIdByAccessKey(accessKey);
        if (id == null || id <= 0) return null;
        User user = innerUserService.getUserById(id);
        // 防止用户 genKeys 后，旧的 AK2ID 缓存未删除导致此方法查出的用户与参数 accessKey 不匹配
        if (!accessKey.equals(user.getAccessKey())) return null;

        return user;
    }

    @Cacheable(
            cacheNames = RedisKey.SC_AK2ID,
            key = "#accessKey",
            sync = true
    )
    public Long getIdByAccessKey(@NonNull String accessKey) {
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("accessKey", accessKey)
                .select("id"));
        return user == null ? null : user.getId();
    }

    @Cacheable(
            cacheNames = RedisKey.SC_USER_INFO,
            key = "#id",
            sync = true
    )
    public User getUserById(@NonNull Long id) {
        return userMapper.selectById(id);
    }

}
