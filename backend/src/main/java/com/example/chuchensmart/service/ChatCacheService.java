package com.example.chuchensmart.service;

import com.example.chuchensmart.common.RedisConstant;
import com.example.chuchensmart.entity.ChatMessage;
import com.example.chuchensmart.entity.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 对话缓存服务
 * 使用 Redis 缓存对话消息和会话信息
 */
@Service
public class ChatCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ChatCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存对话消息
     */
    public void cacheMessage(Long conversationId, ChatMessage message) {
        String key = RedisConstant.buildChatMessageKey(conversationId);
        logger.info("【缓存服务】准备缓存消息到 Redis, key={}, message={}", key, message);

        try {
            ListOperations<String, Object> ops = redisTemplate.opsForList();
            Long result = ops.rightPush(key, message);
            logger.info("【缓存服务】rightPush 返回值: {}", result);

            // 设置过期时间（1天）
            Boolean expireResult = redisTemplate.expire(key, RedisConstant.EXPIRE_24_HOUR, TimeUnit.SECONDS);
            logger.info("【缓存服务】设置过期时间结果: {}", expireResult);

            // 验证存储
            Long size = ops.size(key);
            logger.info("【缓存服务】缓存后消息列表大小: {}", size);
        } catch (Exception e) {
            logger.error("【缓存服务】缓存消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取对话消息列表
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessage> getMessages(Long conversationId) {
        String key = RedisConstant.buildChatMessageKey(conversationId);
        logger.info("【缓存服务】准备从 Redis 获取消息, key={}", key);

        try {
            ListOperations<String, Object> ops = redisTemplate.opsForList();
            Long size = ops.size(key);
            logger.info("【缓存服务】Redis 中消息列表大小: {}", size);

            List<Object> messages = ops.range(key, 0, -1);
            logger.info("【缓存服务】获取到的消息对象数量: {}", messages != null ? messages.size() : 0);

            if (messages == null || messages.isEmpty()) {
                logger.info("【缓存服务】未找到缓存消息");
                return null;
            }

            // 检查是否包含 LinkedHashMap（Redis 反序列化问题）
            boolean hasLinkedHashMap = messages.stream().anyMatch(msg -> msg instanceof Map);
            if (hasLinkedHashMap) {
                logger.warn("【缓存服务】检测到 LinkedHashMap，Redis 缓存数据格式异常，清除缓存并返回 null 从数据库查询");
                // 清除异常的缓存数据
                redisTemplate.delete(key);
                return null;
            }

            // 尝试直接转换为 ChatMessage 列表
            try {
                List<ChatMessage> result = (List<ChatMessage>) (List<?>) messages;
                logger.info("【缓存服务】成功获取 {} 条消息", result.size());
                return result;
            } catch (ClassCastException e) {
                logger.warn("【缓存服务】类型转换失败，返回 null 从数据库查询: {}", e.getMessage());
                return null;
            }
        } catch (Exception e) {
            logger.error("【缓存服务】获取消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 缓存会话信息
     */
    public void cacheConversation(Conversation conversation) {
        String key = RedisConstant.buildConversationKey(conversation.getId());
        redisTemplate.opsForValue().set(key, conversation,
                RedisConstant.EXPIRE_24_HOUR, TimeUnit.SECONDS);
    }

    /**
     * 获取会话信息
     */
    public Conversation getConversation(Long conversationId) {
        String key = RedisConstant.buildConversationKey(conversationId);
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        // 检查是否是 LinkedHashMap（Redis 反序列化问题）
        if (obj instanceof Map) {
            logger.warn("【缓存服务】检测到 LinkedHashMap，Redis 缓存数据格式异常，清除缓存并返回 null");
            redisTemplate.delete(key);
            return null;
        }
        return (Conversation) obj;
    }

    /**
     * 缓存用户会话列表
     */
    public void cacheUserConversations(Long userId, List<Conversation> conversations) {
        String key = RedisConstant.buildUserConversationsKey(userId);
        redisTemplate.opsForValue().set(key, conversations,
                RedisConstant.EXPIRE_24_HOUR, TimeUnit.SECONDS);
    }

    /**
     * 获取用户会话列表
     */
    @SuppressWarnings("unchecked")
    public List<Conversation> getUserConversations(Long userId) {
        String key = RedisConstant.buildUserConversationsKey(userId);
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj instanceof List) {
            // 检查是否包含 LinkedHashMap（Redis 反序列化问题）
            List<?> list = (List<?>) obj;
            boolean hasLinkedHashMap = list.stream().anyMatch(item -> item instanceof Map);
            if (hasLinkedHashMap) {
                logger.warn("【缓存服务】检测到 LinkedHashMap，Redis 缓存数据格式异常，清除缓存并返回 null");
                redisTemplate.delete(key);
                return null;
            }
            return (List<Conversation>) obj;
        }
        return null;
    }

    /**
     * 删除对话缓存
     */
    public void deleteConversationCache(Long conversationId) {
        String messageKey = RedisConstant.buildChatMessageKey(conversationId);
        String conversationKey = RedisConstant.buildConversationKey(conversationId);

        redisTemplate.delete(messageKey);
        redisTemplate.delete(conversationKey);
    }
}
