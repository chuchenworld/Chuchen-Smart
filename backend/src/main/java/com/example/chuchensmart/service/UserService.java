package com.example.chuchensmart.service;

import com.example.chuchensmart.entity.User;

/**
 * 用户服务接口
 * @author 小李
 */
public interface UserService {

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户对象
     */
    User getById(Long id);

    /**
     * 保存用户
     * @param user 用户对象
     * @return 保存后的用户对象
     */
    User save(User user);
}
