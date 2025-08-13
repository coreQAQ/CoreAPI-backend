package com.core.coreapi.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮箱绑定请求
 */
@Data
public class EmailBindRequest implements Serializable {

    /**
     * 邮箱
     */
    private String email;

    /**
     * 验证码
     */
    private String captcha;

    private static final long serialVersionUID = 1L;

}
