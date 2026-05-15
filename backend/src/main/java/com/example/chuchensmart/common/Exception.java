package com.example.chuchensmart.common;

/**
 * 自定义异常类
 *
 * @author 小李
 * @create 2026-05-11
 */
public class Exception extends RuntimeException {

    private Integer code;

    public Exception(String message) {
        super(message);
        this.code = 500;
    }

    public Exception(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Exception(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
