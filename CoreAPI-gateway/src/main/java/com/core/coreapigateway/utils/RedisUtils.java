package com.core.coreapigateway.utils;

import cn.hutool.core.io.resource.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;

public class RedisUtils {

    /**
     * 加载 lua 脚本
     * @param scriptName
     * @return
     */
    public static String loadScript(String scriptName) {
        try (InputStream inputStream = ResourceUtil.getResource("scripts/" + scriptName + ".lua").openStream()){
            if (inputStream == null)
                throw new IllegalStateException("Script not found: " + scriptName);
            return new String(inputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load script: " + scriptName, e);
        }
    }

}
