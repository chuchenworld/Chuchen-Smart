package com.example.chuchensmart.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * @author 小李
 * @Description 存储用户和AI的对话历史
 * @create 2026-05-12
 */
@Data
@Builder
@NoArgsConstructor
public class ChatMessage {

    @JsonCreator
    public ChatMessage(
            @JsonProperty("id") Long id,
            @JsonProperty("conversationId") Long conversationId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("role") String role,
            @JsonProperty("content") String content,
            @JsonProperty("tokens") Integer tokens,
            @JsonProperty("responseTime") Integer responseTime,
            @JsonProperty("status") Integer status,
            @JsonProperty("createdAt") java.time.LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.tokens = tokens;
        this.responseTime = responseTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 用户ID */
    private Long userId;

    /** 消息角色: user, assistant, system */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消耗的token数量 */
    private Integer tokens;

    /** 响应时间(毫秒) */
    private Integer responseTime;

    /** 状态: 1-正常, 0-删除 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
