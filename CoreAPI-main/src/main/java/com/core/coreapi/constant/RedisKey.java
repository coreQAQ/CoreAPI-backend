package com.core.coreapi.constant;

public interface RedisKey {

    String base = "CoreAPI:";
    String CAPTCHA = base + "captcha:%s"; // 验证码 email为key
    String SEND_LIMIT = base + "captcha:send_limit:%s"; // 发送验证码频控 email为key
    String BLACKLIST = base + "blacklist";

    String USER_INFO = base + "user:info:"; // 用户信息
    String API_INFO = base + "api:info:"; // 接口信息
    String AK2ID = base + "user:ak2id:"; // ak 和 id 映射
    String PM2ID = base + "api:pm2id:"; // path + method 和 id 映射

    // region lock key
    String lockBase = base + "lock:";
    String CAPTCHA_LOCK = lockBase + "captcha:%s";

    // spring cache
    String SC_USER_INFO = "user:info"; // 用户信息
    String SC_API_INFO = "api:info"; // 接口信息
    String SC_AK2ID = "user:ak2id"; // ak 和 id 映射
    String SC_PM2ID = "api:pm2id"; // path + method 和 id 映射

    // endregion
}
