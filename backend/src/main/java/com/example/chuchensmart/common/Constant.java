package com.example.chuchensmart.common;

/**
 * 常量类
 *
 * @author 小李
 * @create 2026-05-11
 */
public class Constant {

    /**
     * JWT相关常量
     */
    public static final String JWT_SECRET = "chuchensmartsecretkey2026";
    public static final long JWT_EXPIRATION = 86400000L; // 24小时

    /**
     * 用户相关常量
     */
    public static final String USER_SESSION_KEY = "user_session";
    public static final String USER_ROLE_ADMIN = "ADMIN";
    public static final String USER_ROLE_USER = "USER";

    /**
     * 缓存相关常量
     */
    public static final String CACHE_KEY_PREFIX = "chuchensmart:";
    public static final long CACHE_EXPIRATION = 3600L; // 1小时

    /**
     * 文件上传相关常量
     */
    public static final String UPLOAD_PATH = "/tmp/uploads";
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    /**
     * 系统相关常量
     */
    public static final String SYSTEM_NAME = "Chuchen Smart";
    public static final String SYSTEM_VERSION = "1.0.0";

    /**
     * 消息相关常量
     */
    public static final String MESSAGE_SUCCESS = "操作成功";
    public static final String MESSAGE_ERROR = "操作失败";
    public static final String MESSAGE_PARAM_ERROR = "参数错误";
    public static final String MESSAGE_NOT_FOUND = "数据不存在";
    public static final String MESSAGE_UNAUTHORIZED = "未授权";
    public static final String MESSAGE_FORBIDDEN = "无权限";

    private Constant() {
        // 防止实例化
    }
}
