package com.example.chuchensmart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 性能统计实体类
 * @author 小李
 * @Description 用于监控和优化AI性能
 * @create 2026-05-12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPerformance {

    /** 统计ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** AI模型 */
    private String model;

    /** 请求次数 */
    private Integer requestCount;

    /** 总token数 */
    private Integer totalTokens;

    /** 平均响应时间(毫秒) */
    private Integer avgResponseTime;

    /** 成功次数 */
    private Integer successCount;

    /** 错误次数 */
    private Integer errorCount;

    /** 统计日期 */
    private LocalDate statisticsDate;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
