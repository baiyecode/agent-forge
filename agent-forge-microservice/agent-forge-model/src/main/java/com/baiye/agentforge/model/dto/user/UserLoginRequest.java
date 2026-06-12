package com.baiye.agentforge.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserLoginRequest
 * Package: com.baiye.agentforge.model.dto.user
 * Description: 用户登录请求
 *
 * @Author 白夜
 * @Create 2026/5/3 20:28
 * @Version 1.0
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}

