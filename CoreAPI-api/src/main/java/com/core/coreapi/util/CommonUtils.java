package com.core.coreapi.util;

public class CommonUtils {

    public static boolean isSupportedType(String type) {
        return switch (type) {
            case "email", "mobile", "chinese", "idcard", "url",
                 "mac", "ipv4", "ipv6", "zipcode", "birthday",
                 "plate" -> true;
            default -> false;
        };
    }

}
