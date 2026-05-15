package com.example.chuchensmart.service;

import com.example.chuchensmart.common.RedisConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户行为统计服务
 * 使用 Redis ZSet 记录用户行为，支持按时间查询
 */
@Service
public class UserBehaviorService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 记录用户行为
     * @param userId 用户ID
     * @param actionType 行为类型（如：send_message, view_conversation）
     */
    public void recordBehavior(Long userId, String actionType) {
        String key = RedisConstant.USER_BEHAVIOR_PREFIX + userId + ":" + actionType;

        // 使用 ZSet 记录时间戳和计数
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        String timestamp = String.valueOf(System.currentTimeMillis());
        ops.add(key, timestamp, System.currentTimeMillis());

        // 设置过期时间（30天）
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    /**
     * 记录用户行为（带数据）
     */
    public void recordBehavior(Long userId, String actionType, String data) {
        String key = RedisConstant.USER_BEHAVIOR_PREFIX + userId + ":" + actionType;

        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String value = timestamp + ":" + data;
        ops.add(key, value, System.currentTimeMillis());

        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    /**
     * 增加行为计数（按天统计）
     */
    public void incrementBehaviorCount(Long userId, String actionType) {
        String date = LocalDateTime.now().format(DATE_FORMATTER);
        String key = RedisConstant.USER_BEHAVIOR_COUNT_PREFIX + userId + ":" + actionType + ":" + date;

        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    /**
     * 获取用户行为统计（按天）
     */
    public long getBehaviorCountByDay(Long userId, String actionType, String date) {
        String key = RedisConstant.USER_BEHAVIOR_COUNT_PREFIX + userId + ":" + actionType + ":" + date;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? (Long) count : 0L;
    }

    /**
     * 获取用户最近行为
     */
    public Set<Object> getRecentBehaviors(Long userId, String actionType, int count) {
        String key = RedisConstant.USER_BEHAVIOR_PREFIX + userId + ":" + actionType;
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();

        return ops.reverseRange(key, 0, count - 1);
    }

    /**
     * 获取用户总行为次数（当天）
     */
    public long getTotalBehaviorCount(Long userId, String actionType) {
        String date = LocalDateTime.now().format(DATE_FORMATTER);
        return getBehaviorCountByDay(userId, actionType, date);
    }
}
