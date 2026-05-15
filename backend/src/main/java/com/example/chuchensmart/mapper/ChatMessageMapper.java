/**
 * @author 小李
 * @Description
 * @create 2026-05-11 14:51
 */

package com.example.chuchensmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chuchensmart.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
