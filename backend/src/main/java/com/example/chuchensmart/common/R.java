package com.example.chuchensmart.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果封装
 *
 * @author 小李
 * @create 2026-05-11
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功码
     */
    public static final int SUCCESS_CODE = 200;

    /**
     * 失败码
     */
    public static final int ERROR_CODE = 500;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public R() {
    }

    private R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> success() {
        return new R<>(SUCCESS_CODE, "操作成功", null);
    }

    /**
     * 成功响应（带消息）
     */
    public static <T> R<T> success(String message) {
        return new R<>(SUCCESS_CODE, message, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> success(T data) {
        return new R<>(SUCCESS_CODE, "操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> R<T> success(String message, T data) {
        return new R<>(SUCCESS_CODE, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> R<T> error() {
        return new R<>(ERROR_CODE, "操作失败", null);
    }

    /**
     * 失败响应（带消息）
     */
    public static <T> R<T> error(String message) {
        return new R<>(ERROR_CODE, message, null);
    }

    /**
     * 失败响应（带自定义错误码和消息）
     */
    public static <T> R<T> error(Integer code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == SUCCESS_CODE;
    }
}
