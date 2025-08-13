package com.core.coreapi.controller;

import com.core.coreapi.annotation.AuthCheck;
import com.core.coreapi.constant.RedisKey;
import com.core.coreapi.constant.UserConstant;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.shared.common.BaseResponse;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.common.ResultUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Resource
    private RedissonClient client;

    @GetMapping("/blacklist")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Set<String>> getBlacklist() {
        RSet<String> set = client.getSet(RedisKey.BLACKLIST);
        return ResultUtils.success(set.readAll());
    }

    @PostMapping("/blacklist")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addBlacklist(@RequestBody String ip) {
        if (ip != null && ip.length() > 1 && ip.startsWith("\"") && ip.endsWith("\"")) {
            ip = ip.substring(1, ip.length() - 1);
        }
        RSet<String> set = client.getSet(RedisKey.BLACKLIST);
        set.add(ip);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/blacklist")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBlacklist(@RequestBody String ip) {
        if (ip != null && ip.length() > 1 && ip.startsWith("\"") && ip.endsWith("\"")) {
            ip = ip.substring(1, ip.length() - 1);
        }
        RSet<String> set = client.getSet(RedisKey.BLACKLIST);
        if (set.contains(ip)) {
            set.remove(ip);
            return ResultUtils.success(true);
        }
        else
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
    }

}
