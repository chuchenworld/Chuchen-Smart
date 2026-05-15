package com.example.chuchensmart.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

/**
 * 聊天请求 DTO
 * @author 小李
 * @create 2026-05-13
 */
@Data
public class ChatRequest {

    /**
     * 会话ID（可选，新建会话时为空）
     * 支持字符串格式，避免前端 JavaScript 精度丢失问题
     */
    private Long conversationId;

    /**
     * 用户ID（支持字符串格式）
     */
    private Long userId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * AI模型（可选）
     */
    private String model;

    /**
     * 温度参数（可选）
     */
    private Double temperature;

    /**
     * 设置 conversationId，支持字符串转换
     */
    @JsonSetter("conversationId")
    public void setConversationId(String conversationId) {
        if (conversationId != null && !conversationId.isEmpty()) {
            try {
                this.conversationId = Long.parseLong(conversationId);
            } catch (NumberFormatException e) {
                this.conversationId = null;
            }
        }
    }

    /**
     * 设置 conversationId，支持 Long 类型
     */
    @JsonSetter
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

}
