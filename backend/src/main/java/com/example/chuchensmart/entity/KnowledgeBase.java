package com.example.chuchensmart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库实体类
 * @author 小李
 * @Description 存储AI需要检索的知识内容
 * @create 2026-05-12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBase {

    /** 知识ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 知识标题 */
    private String title;

    /** 知识内容 */
    private String content;

    /** 分类 */
    private String category;

    /** 标签(逗号分隔) */
    private String tags;

    /** 是否公开 */
    private Boolean isPublic;

    /** 浏览次数 */
    private Integer viewCount;

    /** 状态: 1-正常, 0-删除 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
