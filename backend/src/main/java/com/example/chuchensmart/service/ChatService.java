package com.example.chuchensmart.service;

import com.example.chuchensmart.dto.ChatRequest;
import com.example.chuchensmart.dto.ChatResponse;
import com.example.chuchensmart.entity.ChatMessage;
import com.example.chuchensmart.entity.Conversation;

import java.util.List;
import java.util.function.Consumer;

/**
 * 聊天服务接口
 * @author 小李
 */
public interface ChatService {

    /**
     * 发送消息并获取AI回复
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse sendMessage(ChatRequest request);

    /**
     * 发送消息并获取AI回复（流式）
     * @param request 聊天请求
     * @param chunkHandler 数据块处理器
     * @return 聊天响应
     */
    ChatResponse sendMessageStream(ChatRequest request, Consumer<String> chunkHandler);

    /**
     * 创建新会话
     * @param userId 用户ID
     * @param title 会话标题
     * @return 会话对象
     */
    Conversation createConversation(Long userId, String title);

    /**
     * 获取会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> getConversations(Long userId);

    /**
     * 获取会话消息列表
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> getMessages(Long conversationId);

    /**
     * 删除会话
     * @param conversationId 会话ID
     * @return 是否删除成功
     */
    boolean deleteConversation(Long conversationId);
}
