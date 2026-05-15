package com.example.chuchensmart.service;

import com.example.chuchensmart.common.RedisConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户画像服务
 * 使用 Redis Hash 存储用户偏好和画像数据
 */
@Service
public class UserProfileService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 更新用户偏好
     */
    public void updatePreferences(Long userId, Map<String, Object> preferences) {
        String key = RedisConstant.USER_PREFERENCES_PREFIX + userId;
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();

        ops.putAll(key, preferences);
        redisTemplate.expire(key, RedisConstant.EXPIRE_24_HOUR, TimeUnit.SECONDS);
    }

    /**
     * 获取用户偏好
     */
    public Map<Object, Object> getPreferences(Long userId) {
        String key = RedisConstant.USER_PREFERENCES_PREFIX + userId;
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }

    /**
     * 设置单个偏好
     */
    public void setPreference(Long userId, String key, Object value) {
        String redisKey = RedisConstant.USER_PREFERENCES_PREFIX + userId;
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        ops.put(redisKey, key, value);
        redisTemplate.expire(redisKey, RedisConstant.EXPIRE_24_HOUR, TimeUnit.SECONDS);
    }

    /**
     * 获取单个偏好
     */
    public Object getPreference(Long userId, String key) {
        String redisKey = RedisConstant.USER_PREFERENCES_PREFIX + userId;
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        return ops.get(redisKey, key);
    }

    /**
     * 缓存用户画像
     */
    public void cacheUserProfile(Long userId, Map<String, Object> profile) {
        String key = RedisConstant.buildUserProfileKey(userId);
        redisTemplate.opsForHash().putAll(key, profile);
        redisTemplate.expire(key, RedisConstant.EXPIRE_24_HOUR, TimeUnit.SECONDS);
    }

    /**
     * 获取用户画像
     */
    public Map<Object, Object> getUserProfile(Long userId) {
        String key = RedisConstant.buildUserProfileKey(userId);
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }
}
