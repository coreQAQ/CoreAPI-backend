package com.core.coreapi.common;

import java.nio.file.Files;
import java.nio.file.Path;

public class CommonUtils {

    /**
     * 将路径中的占位符替换为正则表达式
     * @param path 原始路径，例如 "/v1/users/{userId}/posts/{postId}"
     * @return 转换后的正则表达式路径，例如 "/v1/users/[^/]+/posts/[^/]+"
     */
    public static String getRegexPath(String path) {
        // 匹配占位符的正则表达式
        String placeholderPattern = "\\{[^/]+\\}";
        // 替换为正则表达式部分
        return path.replaceAll(placeholderPattern, "[^/]+");
    }

    /**
     * 使用Jackson时转义 / 和 ~ 符号
     * @param path
     * @return
     */
    public static String escapeJsonPointer(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }

    /**
     * 首字母小写
     * @param str
     * @return
     */
    public static String lowercaseFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * 根据引用获得schemaName
     * @param ref
     * @return
     */
    public static String getSchemaName(String ref) {
        return ref.replace("#/components/schemas/", "");
    }


    public static boolean safeDelete(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (Exception e) {
            return false;
        }
    }

}