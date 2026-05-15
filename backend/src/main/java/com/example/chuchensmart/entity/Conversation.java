package com.example.chuchensmart.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话会话实体类
 * @author 小李
 * @Description 管理AI对话的上下文和配置
 * @create 2026-05-12
 */
@Data
@Builder
@NoArgsConstructor
public class Conversation {

    @JsonCreator
    public Conversation(
            @JsonProperty("id") Long id,
            @JsonProperty("userId") Long userId,
            @JsonProperty("title") String title,
            @JsonProperty("model") String model,
            @JsonProperty("temperature") Double temperature,
            @JsonProperty("contextWindow") Integer contextWindow,
            @JsonProperty("status") Integer status,
            @JsonProperty("createdAt") java.time.LocalDateTime createdAt,
            @JsonProperty("updatedAt") java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.model = model;
        this.temperature = temperature;
        this.contextWindow = contextWindow;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 会话ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 会话标题 */
    private String title;

    /** 使用的AI模型 */
    private String model;

    /** 温度参数 */
    private Double temperature;

    /** 上下文窗口大小 */
    private Integer contextWindow;

    /** 状态: 1-活跃, 0-归档 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
