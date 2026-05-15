package com.example.chuchensmart.controller;

import com.example.chuchensmart.common.R;
import com.example.chuchensmart.entity.KnowledgeBase;
import com.example.chuchensmart.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 * @author 小李
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseController.class);

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 添加知识库条目
     */
    @PostMapping
    public R<KnowledgeBase> addKnowledge(@RequestBody KnowledgeBase knowledge) {
        logger.info("【知识库管理】添加知识库条目，用户ID: {}, 标题: {}", knowledge.getUserId(), knowledge.getTitle());

        try {
            KnowledgeBase result = knowledgeBaseService.addKnowledge(knowledge);
            return R.success("知识库条目添加成功", result);
        } catch (Exception e) {
            logger.error("【知识库管理】添加知识库条目失败: {}", e.getMessage(), e);
            return R.error("添加知识库条目失败: " + e.getMessage());
        }
    }

    /**
     * 搜索知识库（关键词搜索）
     */
    @GetMapping("/search")
    public R<List<KnowledgeBase>> searchKnowledge(
            @RequestParam Long userId,
            @RequestParam String keyword) {
        logger.info("【知识库管理】搜索知识库，用户ID: {}, 关键词: {}", userId, keyword);

        try {
            List<KnowledgeBase> results = knowledgeBaseService.searchByKeyword(userId, keyword);
            return R.success(results);
        } catch (Exception e) {
            logger.error("【知识库管理】搜索知识库失败: {}", e.getMessage(), e);
            return R.error("搜索知识库失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户知识库列表
     */
    @GetMapping
    public R<List<KnowledgeBase>> getUserKnowledge(@RequestParam Long userId) {
        logger.info("【知识库管理】获取用户知识库列表，用户ID: {}", userId);

        try {
            List<KnowledgeBase> knowledgeList = knowledgeBaseService.getUserKnowledge(userId);
            return R.success(knowledgeList);
        } catch (Exception e) {
            logger.error("【知识库管理】获取用户知识库列表失败: {}", e.getMessage(), e);
            return R.error("获取用户知识库列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除知识库条目
     */
    @DeleteMapping("/{knowledgeId}")
    public R<String> deleteKnowledge(@PathVariable Long knowledgeId) {
        logger.info("【知识库管理】删除知识库条目，ID: {}", knowledgeId);

        try {
            boolean success = knowledgeBaseService.deleteKnowledge(knowledgeId);
            if (success) {
                return R.success("知识库条目删除成功");
            } else {
                return R.error("知识库条目不存在或删除失败");
            }
        } catch (Exception e) {
            logger.error("【知识库管理】删除知识库条目失败: {}", e.getMessage(), e);
            return R.error("删除知识库条目失败: " + e.getMessage());
        }
    }
}
