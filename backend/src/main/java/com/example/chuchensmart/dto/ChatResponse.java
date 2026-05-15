package com.example.chuchensmart.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天响应 DTO
 * @author 小李
 * @create 2026-05-13
 */
@Data
public class ChatResponse {

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息角色
     */
    private String role;

    /**
     * 消耗的token数量
     */
    private Integer tokens;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
