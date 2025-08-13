package com.core.coreapi.model.dto.user;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户注册请求体
 *
 */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String email;

    private String userPassword;

    private String checkPassword;

    private String captcha;
}
