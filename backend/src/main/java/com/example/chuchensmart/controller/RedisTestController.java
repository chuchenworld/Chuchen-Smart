package com.example.chuchensmart.controller;

import com.example.chuchensmart.common.R;
import com.example.chuchensmart.entity.ChatMessage;
import com.example.chuchensmart.entity.Conversation;
import com.example.chuchensmart.service.ChatCacheService;
import com.example.chuchensmart.service.RedisService;
import com.example.chuchensmart.utils.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 测试控制器
 * 用于验证 Redis 连接和存储功能
 */
@RestController
@RequestMapping("/api/redis/test")
public class RedisTestController {

    private static final Logger logger = LoggerFactory.getLogger(RedisTestController.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private ChatCacheService chatCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 测试 Redis 连接
     */
    @GetMapping("/ping")
    public R<String> ping() {
        try {
            String result = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                return connection.ping();
            });
            logger.info("Redis ping 成功: {}", result);
            return R.success("Redis 连接正常: " + result);
        } catch (Exception e) {
            logger.error("Redis 连接失败: {}", e.getMessage());
            return R.error("Redis 连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试存储简单值
     */
    @PostMapping("/set-simple")
    public R<String> setSimple(@RequestParam String key, @RequestParam String value) {
        try {
            redisService.set(key, value, 60, TimeUnit.SECONDS);
            logger.info("存储简单值成功: key={}, value={}", key, value);
            return R.success("存储成功: " + key + "=" + value);
        } catch (Exception e) {
            logger.error("存储简单值失败: {}", e.getMessage());
            return R.error("存储失败: " + e.getMessage());
        }
    }

    /**
     * 测试获取简单值
     */
    @GetMapping("/get-simple")
    public R<String> getSimple(@RequestParam String key) {
        try {
            Object value = redisService.get(key);
            logger.info("获取简单值成功: key={}, value={}", key, value);
            return R.success("获取成功: " + key + "=" + value);
        } catch (Exception e) {
            logger.error("获取简单值失败: {}", e.getMessage());
            return R.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 测试存储对话消息
     */
    @PostMapping("/set-chat-message")
    public R<String> setChatMessage(@RequestParam Long conversationId) {
        try {
            logger.info("开始创建 ChatMessage 对象...");
            ChatMessage message = ChatMessage.builder()
                    .id(IdGenerator.generateId())
                    .conversationId(conversationId)
                    .userId(1L)
                    .role("user")
                    .content("测试消息")
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .build();
            logger.info("ChatMessage 对象创建成功: {}", message);

            logger.info("开始调用 cacheMessage...");
            chatCacheService.cacheMessage(conversationId, message);
            logger.info("存储对话消息成功: conversationId={}, messageId={}", conversationId, message.getId());

            // 验证是否存储成功
            var messages = chatCacheService.getMessages(conversationId);
            logger.info("验证存储结果: 获取到 {} 条消息", messages != null ? messages.size() : 0);

            return R.success("存储对话消息成功: messageId=" + message.getId());
        } catch (Exception e) {
            logger.error("存储对话消息失败: {}", e.getMessage(), e);
            logger.error("完整错误堆栈:", e);
            return R.error("存储对话消息失败: " + e.getMessage());
        }
    }

    /**
     * 测试获取对话消息
     */
    @GetMapping("/get-chat-messages")
    public R<String> getChatMessages(@RequestParam Long conversationId) {
        try {
            var messages = chatCacheService.getMessages(conversationId);
            if (messages == null || messages.isEmpty()) {
                logger.info("未找到对话消息: conversationId={}", conversationId);
                return R.success("未找到对话消息");
            }
            logger.info("获取对话消息成功: conversationId={}, 数量={}", conversationId, messages.size());
            return R.success("获取对话消息成功: " + messages.size() + " 条");
        } catch (Exception e) {
            logger.error("获取对话消息失败: {}", e.getMessage(), e);
            return R.error("获取对话消息失败: " + e.getMessage());
        }
    }

    /**
     * 测试存储会话
     */
    @PostMapping("/set-conversation")
    public R<String> setConversation(@RequestParam Long conversationId) {
        try {
            Conversation conversation = Conversation.builder()
                    .id(conversationId)
                    .userId(1L)
                    .title("测试会话")
                    .model("qwen2.5-vl-72b-instruct")
                    .temperature(0.7)
                    .contextWindow(2048)
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            chatCacheService.cacheConversation(conversation);
            logger.info("存储会话成功: conversationId={}", conversationId);
            return R.success("存储会话成功: conversationId=" + conversationId);
        } catch (Exception e) {
            logger.error("存储会话失败: {}", e.getMessage(), e);
            return R.error("存储会话失败: " + e.getMessage());
        }
    }

    /**
     * 测试获取会话
     */
    @GetMapping("/get-conversation")
    public R<String> getConversation(@RequestParam Long conversationId) {
        try {
            Conversation conversation = chatCacheService.getConversation(conversationId);
            if (conversation == null) {
                logger.info("未找到会话: conversationId={}", conversationId);
                return R.success("未找到会话");
            }
            logger.info("获取会话成功: conversationId={}, title={}", conversationId, conversation.getTitle());
            return R.success("获取会话成功: title=" + conversation.getTitle());
        } catch (Exception e) {
            logger.error("获取会话失败: {}", e.getMessage(), e);
            return R.error("获取会话失败: " + e.getMessage());
        }
    }

    /**
     * 测试存储用户偏好
     */
    @PostMapping("/set-preferences")
    public R<String> setPreferences(@RequestParam Long userId) {
        try {
            Map<String, Object> preferences = new HashMap<>();
            preferences.put("theme", "dark");
            preferences.put("language", "zh-CN");
            preferences.put("fontSize", 14);

            redisService.set("ai:user:preferences:" + userId, preferences, 3600, TimeUnit.SECONDS);
            logger.info("存储用户偏好成功: userId={}", userId);
            return R.success("存储用户偏好成功");
        } catch (Exception e) {
            logger.error("存储用户偏好失败: {}", e.getMessage(), e);
            return R.error("存储用户偏好失败: " + e.getMessage());
        }
    }

    /**
     * 测试获取用户偏好
     */
    @GetMapping("/get-preferences")
    public R<String> getPreferences(@RequestParam Long userId) {
        try {
            Object preferences = redisService.get("ai:user:preferences:" + userId);
            if (preferences == null) {
                logger.info("未找到用户偏好: userId={}", userId);
                return R.success("未找到用户偏好");
            }
            logger.info("获取用户偏好成功: userId={}, preferences={}", userId, preferences);
            return R.success("获取用户偏好成功: " + preferences);
        } catch (Exception e) {
            logger.error("获取用户偏好失败: {}", e.getMessage(), e);
            return R.error("获取用户偏好失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有 Redis keys（用于调试）
     */
    @GetMapping("/keys")
    public R<String> getKeys(@RequestParam(required = false) String pattern) {
        try {
            String searchPattern = pattern != null ? pattern : "*";
            var keys = redisTemplate.keys(searchPattern);
            if (keys == null || keys.isEmpty()) {
                return R.success("未找到 keys");
            }
            return R.success("找到 " + keys.size() + " 个 keys: " + keys);
        } catch (Exception e) {
            logger.error("获取 keys 失败: {}", e.getMessage());
            return R.error("获取 keys 失败: " + e.getMessage());
        }
    }

    /**
     * 删除 key
     */
    @DeleteMapping("/delete")
    public R<String> deleteKey(@RequestParam String key) {
        try {
            redisTemplate.delete(key);
            logger.info("删除 key 成功: {}", key);
            return R.success("删除 key 成功: " + key);
        } catch (Exception e) {
            logger.error("删除 key 失败: {}", e.getMessage());
            return R.error("删除 key 失败: " + e.getMessage());
        }
    }
}
