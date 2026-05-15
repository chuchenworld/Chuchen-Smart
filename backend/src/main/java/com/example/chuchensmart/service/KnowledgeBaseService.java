package com.example.chuchensmart.service;

import com.example.chuchensmart.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 * @author 小李
 */
public interface KnowledgeBaseService {

    /**
     * 添加知识库条目
     * @param knowledge 知识库对象
     * @return 添加后的知识库对象
     */
    KnowledgeBase addKnowledge(KnowledgeBase knowledge);

    /**
     * 关键词搜索知识库
     * @param userId 用户ID
     * @param keyword 关键词
     * @return 搜索结果列表
     */
    List<KnowledgeBase> searchByKeyword(Long userId, String keyword);

    /**
     * 向量搜索知识库
     * @param userId 用户ID
     * @param query 查询文本
     * @return 搜索结果列表
     */
    List<KnowledgeBase> searchByVector(Long userId, String query);

    /**
     * 删除知识库条目
     * @param knowledgeId 知识库ID
     * @return 是否删除成功
     */
    boolean deleteKnowledge(Long knowledgeId);

    /**
     * 获取用户知识库列表
     * @param userId 用户ID
     * @return 知识库列表
     */
    List<KnowledgeBase> getUserKnowledge(Long userId);
}
