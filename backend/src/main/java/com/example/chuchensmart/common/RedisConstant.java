package com.example.chuchensmart.common;

/**
 * Redis 常量类
 * 定义 Redis Key 前缀和过期时间
 */
public class RedisConstant {

    // 对话缓存
    public static final String CHAT_MESSAGE_PREFIX = "ai:chat:message:";
    public static final String CONVERSATION_PREFIX = "ai:conversation:";
    public static final String USER_CONVERSATIONS_PREFIX = "ai:user:conversations:";

    // 用户行为统计
    public static final String USER_BEHAVIOR_PREFIX = "ai:user:behavior:";
    public static final String USER_BEHAVIOR_COUNT_PREFIX = "ai:user:behavior:count:";

    // 用户画像
    public static final String USER_PROFILE_PREFIX = "ai:user:profile:";
    public static final String USER_PREFERENCES_PREFIX = "ai:user:preferences:";

    // 过期时间（秒）
    public static final long EXPIRE_1_HOUR = 3600L;
    public static final long EXPIRE_24_HOUR = 86400L;
    public static final long EXPIRE_7_DAYS = 604800L;

    /**
     * 构建对话消息 Key
     */
    public static String buildChatMessageKey(Long conversationId) {
        return CHAT_MESSAGE_PREFIX + conversationId;
    }

    /**
     * 构建会话 Key
     */
    public static String buildConversationKey(Long conversationId) {
        return CONVERSATION_PREFIX + conversationId;
    }

    /**
     * 构建用户会话列表 Key
     */
    public static String buildUserConversationsKey(Long userId) {
        return USER_CONVERSATIONS_PREFIX + userId;
    }

    /**
     * 构建用户画像 Key
     */
    public static String buildUserProfileKey(Long userId) {
        return USER_PROFILE_PREFIX + userId;
    }
}
