package com.core.coreapigateway.constant;

public interface RedisKey {

    String base = "CoreAPI:";
    String API_INFO = "api_info:"; // 接口信息缓存
    String API_USAGE = "api_usage:"; // 接口调用次数缓存
    String USER_INFO = "user_info:"; // 用户信息缓存
    String ACCESS_TOKEN = "access_token:"; // 访问令牌缓存
    String API_KEY = "api_key:"; // API密钥缓存
    String BLACKLIST = base + "blacklist"; // 黑名单缓存
    String RATE_LIMIT = base + "rate_limit:"; // 限流相关缓存
    String NONCE = base + "nonce:%s"; // 随机数防重放

}
