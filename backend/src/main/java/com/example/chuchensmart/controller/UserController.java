package com.example.chuchensmart.controller;

import com.example.chuchensmart.common.R;
import com.example.chuchensmart.entity.User;
import com.example.chuchensmart.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * @author 小李
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) {
        logger.info("【用户管理】查询用户 ID: {}", id);

        try {
            User user = userService.getById(id);
            if (user != null) {
                logger.info("【用户管理】查询用户 ID: {} 成功，用户名: {}", id, user.getUsername());
                return R.success(user);
            } else {
                logger.warn("【用户管理】查询用户 ID: {} 不存在", id);
                return R.error("用户不存在");
            }
        } catch (Exception e) {
            logger.error("【用户管理】查询用户 ID: {} 失败: {}", id, e.getMessage(), e);
            return R.error("查询用户失败: " + e.getMessage());
        }
    }

    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建后的用户信息
     */
    @PostMapping
    public R<User> create(@RequestBody User user) {
        logger.info("【用户管理】创建用户，用户名: {}", user.getUsername());

        try {
            User savedUser = userService.save(user);
            logger.info("【用户管理】创建用户成功，用户ID: {}, 用户名: {}", savedUser.getId(), savedUser.getUsername());
            return R.success("用户创建成功", savedUser);
        } catch (Exception e) {
            logger.error("【用户管理】创建用户失败: {}", e.getMessage(), e);
            return R.error("用户创建失败: " + e.getMessage());
        }
    }
}
