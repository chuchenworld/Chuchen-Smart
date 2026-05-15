package com.example.chuchensmart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 提示模板实体类
 * @author 小李
 * @Description 存储常用的AI提示模板
 * @create 2026-05-12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PromptTemplate {

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    private String name;

    /** 模板描述 */
    private String description;

    /** 模板内容 */
    private String template;

    /** 变量列表(逗号分隔) */
    private String variables;

    /** 分类 */
    private String category;

    /** 使用次数 */
    private Integer usageCount;

    /** 是否默认模板 */
    private Boolean isDefault;

    /** 状态: 1-正常, 0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
