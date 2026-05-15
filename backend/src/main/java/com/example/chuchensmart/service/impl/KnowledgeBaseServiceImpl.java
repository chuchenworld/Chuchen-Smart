package com.example.chuchensmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chuchensmart.agent.LlmClient;
import com.example.chuchensmart.entity.KnowledgeBase;
import com.example.chuchensmart.entity.VectorEmbedding;
import com.example.chuchensmart.mapper.KnowledgeBaseMapper;
import com.example.chuchensmart.mapper.VectorEmbeddingMapper;
import com.example.chuchensmart.service.KnowledgeBaseService;
import com.example.chuchensmart.utils.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库服务实现类
 * @author 小李
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private VectorEmbeddingMapper vectorEmbeddingMapper;

    @Autowired
    private LlmClient llmClient;

    @Override
    @Transactional
    public KnowledgeBase addKnowledge(KnowledgeBase knowledge) {
        logger.info("【知识库服务】添加知识库条目，用户ID: {}, 标题: {}", knowledge.getUserId(), knowledge.getTitle());

        // 生成ID
        knowledge.setId(IdGenerator.generateId());
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledge.setStatus(1);
        knowledge.setViewCount(0);

        // 保存到数据库
        knowledgeBaseMapper.insert(knowledge);
        logger.info("【知识库服务】知识库条目保存成功，ID: {}", knowledge.getId());

        // 生成向量嵌入并保存
        try {
            String embedding = llmClient.generateEmbedding(knowledge.getContent());
            if (embedding != null) {
                VectorEmbedding vectorEmbedding = VectorEmbedding.builder()
                        .id(IdGenerator.generateId())
                        .sourceId(knowledge.getId())
                        .sourceType("knowledge")
                        .embedding(embedding)
                        .contentHash(generateContentHash(knowledge.getContent()))
                        .metadata("{\"title\":\"" + knowledge.getTitle() + "\",\"category\":\"" + knowledge.getCategory() + "\"}")
                        .createdAt(LocalDateTime.now())
                        .build();
                vectorEmbeddingMapper.insert(vectorEmbedding);
                logger.info("【知识库服务】向量嵌入保存成功");
            }
        } catch (Exception e) {
            logger.warn("【知识库服务】生成向量嵌入失败: {}", e.getMessage());
        }

        return knowledge;
    }

    @Override
    public List<KnowledgeBase> searchByKeyword(Long userId, String keyword) {
        logger.info("【知识库服务】关键词搜索，用户ID: {}, 关键词: {}", userId, keyword);

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId)
               .eq(KnowledgeBase::getStatus, 1)
               .like(KnowledgeBase::getTitle, keyword)
               .or()
               .like(KnowledgeBase::getContent, keyword)
               .orderByDesc(KnowledgeBase::getCreatedAt);

        List<KnowledgeBase> results = knowledgeBaseMapper.selectList(wrapper);
        logger.info("【知识库服务】关键词搜索结果: {} 条", results.size());
        return results;
    }

    @Override
    public List<KnowledgeBase> searchByVector(Long userId, String query) {
        logger.info("【知识库服务】向量搜索，用户ID: {}, 查询: {}", userId, query);

        try {
            // 生成查询文本的向量嵌入
            String queryEmbedding = llmClient.generateEmbedding(query);
            if (queryEmbedding == null) {
                logger.warn("【知识库服务】生成查询向量嵌入失败");
                return List.of();
            }

            // 这里应该实现向量相似性搜索
            // 由于MySQL不支持向量搜索，这里使用关键词搜索作为替代
            return searchByKeyword(userId, query);

        } catch (Exception e) {
            logger.error("【知识库服务】向量搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean deleteKnowledge(Long knowledgeId) {
        logger.info("【知识库服务】删除知识库条目，ID: {}", knowledgeId);

        KnowledgeBase knowledge = knowledgeBaseMapper.selectById(knowledgeId);
        if (knowledge == null) {
            logger.warn("【知识库服务】知识库条目不存在: {}", knowledgeId);
            return false;
        }

        // 软删除
        knowledge.setStatus(0);
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(knowledge);

        // 删除对应的向量嵌入
        LambdaQueryWrapper<VectorEmbedding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorEmbedding::getSourceId, knowledgeId)
               .eq(VectorEmbedding::getSourceType, "knowledge");
        vectorEmbeddingMapper.delete(wrapper);

        logger.info("【知识库服务】知识库条目删除成功: {}", knowledgeId);
        return true;
    }

    @Override
    public List<KnowledgeBase> getUserKnowledge(Long userId) {
        logger.info("【知识库服务】获取用户知识库，用户ID: {}", userId);

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getUserId, userId)
               .eq(KnowledgeBase::getStatus, 1)
               .orderByDesc(KnowledgeBase::getCreatedAt);

        List<KnowledgeBase> results = knowledgeBaseMapper.selectList(wrapper);
        logger.info("【知识库服务】用户知识库条目: {} 条", results.size());
        return results;
    }

    /**
     * 生成内容哈希（用于去重）
     */
    private String generateContentHash(String content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }
}
