package com.example.chuchensmart.controller;

import com.example.chuchensmart.common.R;
import com.example.chuchensmart.service.UserBehaviorService;
import com.example.chuchensmart.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Redis 相关 API 控制器
 * 提供用户行为记录和用户画像管理接口
 */
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private UserProfileService userProfileService;

    /**
     * 记录用户行为
     */
    @PostMapping("/behavior/record")
    public R<Void> recordBehavior(@RequestParam Long userId, @RequestParam String actionType) {
        userBehaviorService.recordBehavior(userId, actionType);
        userBehaviorService.incrementBehaviorCount(userId, actionType);
        return R.success();
    }

    /**
     * 获取用户行为统计（当天）
     */
    @GetMapping("/behavior/count")
    public R<Long> getBehaviorCount(@RequestParam Long userId, @RequestParam String actionType) {
        long count = userBehaviorService.getTotalBehaviorCount(userId, actionType);
        return R.success(count);
    }

    /**
     * 更新用户偏好
     */
    @PostMapping("/profile/preferences")
    public R<Void> updatePreferences(@RequestParam Long userId, @RequestBody Map<String, Object> preferences) {
        userProfileService.updatePreferences(userId, preferences);
        return R.success();
    }

    /**
     * 获取用户偏好
     */
    @GetMapping("/profile/preferences")
    public R<Map<Object, Object>> getPreferences(@RequestParam Long userId) {
        Map<Object, Object> preferences = userProfileService.getPreferences(userId);
        return R.success(preferences);
    }

    /**
     * 设置单个用户偏好
     */
    @PostMapping("/profile/preference")
    public R<Void> setPreference(@RequestParam Long userId, @RequestParam String key, @RequestBody Object value) {
        userProfileService.setPreference(userId, key, value);
        return R.success();
    }

    /**
     * 获取单个用户偏好
     */
    @GetMapping("/profile/preference")
    public R<Object> getPreference(@RequestParam Long userId, @RequestParam String key) {
        Object value = userProfileService.getPreference(userId, key);
        return R.success(value);
    }
}
