package com.example.chuchensmart.service.impl;

import com.example.chuchensmart.entity.User;
import com.example.chuchensmart.mapper.UserMapper;
import com.example.chuchensmart.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * @author 小李
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User save(User user) {
        userMapper.insert(user);
        return user;
    }
}
