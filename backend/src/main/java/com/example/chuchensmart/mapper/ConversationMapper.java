package com.example.chuchensmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chuchensmart.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话会话 Mapper
 * @author 小李
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
