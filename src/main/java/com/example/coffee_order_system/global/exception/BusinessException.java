package com.example.coffee_order_system.global.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 발생하는 런타임 예외
 * ErrorCode를 통해 HTTP 상태코드와 에러 메시지를 일관되게 관리한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
