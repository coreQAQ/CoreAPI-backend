package com.core.coreapi.shared.service;

import com.core.coreapi.shared.entity.User;

/**
 * 内部用户服务
 *
 */
public interface InnerUserService {

    /**
     * 根据访问密钥获取用户
     *
     * @param accessKey
     * @return
     */
    public User getUserByAccessKey(String accessKey);

}