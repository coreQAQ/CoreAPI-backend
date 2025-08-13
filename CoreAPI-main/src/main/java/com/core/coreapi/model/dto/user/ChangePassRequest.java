package com.core.coreapi.model.dto.user;

import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ChangePassRequest {

    /**
     * 验证码
     */
    private String captcha;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 新密码
     */
    private String newPassword;

}
