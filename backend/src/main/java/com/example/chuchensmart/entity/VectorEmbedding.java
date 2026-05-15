package com.example.chuchensmart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 向量嵌入实体类
 * @author 小李
 * @Description 用于相似性搜索和RAG(检索增强生成)
 * @create 2026-05-12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VectorEmbedding {

    /** 向量ID */
    private Long id;

    /** 源数据ID */
    private Long sourceId;

    /** 源数据类型: knowledge, message, document */
    private String sourceType;

    /** 向量嵌入(1536维) */
    private String embedding;

    /** 内容哈希(用于去重) */
    private String contentHash;

    /** 元数据(JSON格式) */
    private String metadata;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
