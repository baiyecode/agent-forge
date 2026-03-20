package com.baiye.agentforge.exception;

import lombok.Getter;

/**
 * ClassName: BusinessException
 * Package: com.baiye.agentforge.exception
 * Description: 自定义业务异常
 *
 * @Author 白夜
 * @Create 2026/3/20 14:16
 * @Version 1.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}

