package com.example.chuchensmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chuchensmart.agent.LlmClient;
import com.example.chuchensmart.dto.ChatRequest;
import com.example.chuchensmart.dto.ChatResponse;
import com.example.chuchensmart.entity.ChatMessage;
import com.example.chuchensmart.entity.Conversation;
import com.example.chuchensmart.entity.KnowledgeBase;
import com.example.chuchensmart.mapper.ChatMessageMapper;
import com.example.chuchensmart.mapper.ConversationMapper;
import com.example.chuchensmart.service.ChatCacheService;
import com.example.chuchensmart.service.ChatService;
import com.example.chuchensmart.service.UserBehaviorService;
import com.example.chuchensmart.utils.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 聊天服务实现类
 * @author 小李
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private ChatCacheService chatCacheService;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        logger.info("【聊天服务】处理用户 {} 的消息发送请求", request.getUserId());

        // 记录用户行为
        userBehaviorService.recordBehavior(request.getUserId(), "send_message");
        userBehaviorService.incrementBehaviorCount(request.getUserId(), "send_message");

        Long conversationId = request.getConversationId();
        Conversation conversation = null;

        // 如果没有会话ID，创建新会话
        if (conversationId == null) {
            logger.info("【聊天服务】用户 {} 没有会话ID，创建新会话", request.getUserId());
            conversation = createConversation(request.getUserId(), "新对话");
            conversationId = conversation.getId();
            logger.info("【聊天服务】创建新会话成功，会话ID: {}", conversationId);
        } else {
            conversation = conversationMapper.selectById(conversationId);
            logger.info("【聊天服务】使用现有会话ID: {}", conversationId);

            // 检查会话状态
            if (conversation == null) {
                logger.warn("【聊天服务】会话ID{} 不存在，创建新会话", conversationId);
                conversation = createConversation(request.getUserId(), "新对话");
                conversationId = conversation.getId();
            }
        }

        // 获取会话历史消息（用于 AI 上下文）
        List<ChatMessage> historyMessages = getMessages(conversationId);
        logger.info("【聊天服务】获取会话历史消息，共 {} 条", historyMessages.size());

        // RAG检索：从知识库检索相关知识
        List<KnowledgeBase> relevantKnowledge = searchRelevantKnowledge(request.getUserId(), request.getMessage());
        logger.info("【聊天服务】RAG检索到相关知识 {} 条", relevantKnowledge.size());

        // 构建消息列表（包含历史消息、相关知识和当前消息）
        List<Map<String, Object>> messagesForAI = new ArrayList<>();

        // 添加检索到的相关知识（作为系统消息）
        for (KnowledgeBase knowledge : relevantKnowledge) {
            Map<String, Object> knowledgeMsg = new HashMap<>();
            knowledgeMsg.put("role", "system");
            knowledgeMsg.put("content", "【知识库】" + knowledge.getTitle() + ": " + knowledge.getContent());
            messagesForAI.add(knowledgeMsg);
        }

        // 添加历史消息（限制数量，避免 token 超限）
        int maxHistoryMessages = 16; // 最多 16 条历史消息（8轮对话）
        int startIndex = Math.max(0, historyMessages.size() - maxHistoryMessages);

        for (int i = startIndex; i < historyMessages.size(); i++) {
            ChatMessage msg = historyMessages.get(i);
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("role", msg.getRole());
            msgMap.put("content", msg.getContent());
            messagesForAI.add(msgMap);
        }

        // 添加当前用户消息
        Map<String, Object> currentMsg = new HashMap<>();
        currentMsg.put("role", "user");
        currentMsg.put("content", request.getMessage());
        messagesForAI.add(currentMsg);

        logger.info("【聊天服务】发送消息列表到 AI，包含 {} 条消息（{} 条知识 + {} 条历史 + 1 条当前）",
            messagesForAI.size(), relevantKnowledge.size(), messagesForAI.size() - relevantKnowledge.size() - 1);

        // 保存用户消息到数据库（在调用 AI 之前）
        ChatMessage userMessage = ChatMessage.builder()
                .id(IdGenerator.generateId())
                .conversationId(conversationId)
                .userId(request.getUserId())
                .role("user")
                .content(request.getMessage())
                .status(1)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageMapper.insert(userMessage);
        logger.info("【聊天服务】用户消息已保存，消息ID: {}", userMessage.getId());

        // 缓存用户消息到 Redis
        chatCacheService.cacheMessage(conversationId, userMessage);
        logger.info("【聊天服务】用户消息已缓存到 Redis");

        // 调用阿里云百炼 API 获取AI回复（包含历史消息）
        logger.info("【聊天服务】调用阿里云百炼 API（带历史），模型: qwen2.5-vl-72b-instruct");
        long startTime = System.currentTimeMillis();
        String aiResponse = llmClient.sendMessageWithHistory(messagesForAI);
        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("【聊天服务】AI回复生成成功，响应时间: {}ms", responseTime);

        // 保存AI回复到数据库
        ChatMessage assistantMessage = ChatMessage.builder()
                .id(IdGenerator.generateId())
                .conversationId(conversationId)
                .userId(request.getUserId())
                .role("assistant")
                .content(aiResponse)
                .tokens(estimateTokens(aiResponse)) // 估算token数量
                .responseTime((int) responseTime)
                .status(1)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageMapper.insert(assistantMessage);
        logger.info("【聊天服务】AI回复已保存，消息ID: {}, Token数: {}", assistantMessage.getId(), assistantMessage.getTokens());

        // 缓存AI回复到 Redis
        chatCacheService.cacheMessage(conversationId, assistantMessage);
        logger.info("【聊天服务】AI回复已缓存到 Redis");

        // 缓存会话信息
        chatCacheService.cacheConversation(conversation);
        logger.info("【聊天服务】会话信息已缓存到 Redis");

        // 更新会话标题（如果是第一条消息）
        if (conversation.getTitle().equals("新对话")) {
            String title = request.getMessage().length() > 20
                ? request.getMessage().substring(0, 20)
                : request.getMessage();
            conversation.setTitle(title);
            conversationMapper.updateById(conversation);
            logger.info("【聊天服务】更新会话标题为: {}", title);
        }

        // 自动提取知识并存储到知识库
        try {
            extractKnowledge(request.getUserId(), request.getMessage(), aiResponse);
        } catch (Exception e) {
            logger.warn("【聊天服务】知识提取失败: {}", e.getMessage());
        }

        // 构建响应
        ChatResponse response = new ChatResponse();
        response.setConversationId(conversationId);
        response.setMessageId(assistantMessage.getId());
        response.setContent(aiResponse);
        response.setRole("assistant");
        response.setTokens(assistantMessage.getTokens());
        response.setResponseTime(assistantMessage.getResponseTime());
        response.setCreatedAt(assistantMessage.getCreatedAt());

        logger.info("【聊天服务】消息处理完成，会话ID: {}, 消息ID: {}", conversationId, assistantMessage.getId());
        return response;
    }

    @Override
    @Transactional
    public ChatResponse sendMessageStream(ChatRequest request, Consumer<String> chunkHandler) {
        logger.info("【聊天服务】处理用户 {} 的流式消息发送请求", request.getUserId());

        // 记录用户行为
        userBehaviorService.recordBehavior(request.getUserId(), "send_message");
        userBehaviorService.incrementBehaviorCount(request.getUserId(), "send_message");

        Long conversationId = request.getConversationId();
        Conversation conversation = null;

        // 如果没有会话ID，创建新会话
        if (conversationId == null) {
            logger.info("【聊天服务】用户 {} 没有会话ID，创建新会话", request.getUserId());
            conversation = createConversation(request.getUserId(), "新对话");
            conversationId = conversation.getId();
            logger.info("【聊天服务】创建新会话成功，会话ID: {}", conversationId);
        } else {
            conversation = conversationMapper.selectById(conversationId);
            logger.info("【聊天服务】使用现有会话ID: {}", conversationId);

            // 检查会话状态
            if (conversation == null) {
                logger.warn("【聊天服务】会话ID{} 不存在，创建新会话", conversationId);
                conversation = createConversation(request.getUserId(), "新对话");
                conversationId = conversation.getId();
            }
        }

        // 获取会话历史消息（用于 AI 上下文）
        List<ChatMessage> historyMessages = getMessages(conversationId);
        logger.info("【聊天服务】获取会话历史消息，共 {} 条", historyMessages.size());

        // RAG检索：从知识库检索相关知识
        List<KnowledgeBase> relevantKnowledge = searchRelevantKnowledge(request.getUserId(), request.getMessage());
        logger.info("【聊天服务】RAG检索到相关知识 {} 条", relevantKnowledge.size());

        // 构建消息列表（包含历史消息、相关知识和当前消息）
        List<Map<String, Object>> messagesForAI = new ArrayList<>();

        // 添加检索到的相关知识（作为系统消息）
        for (KnowledgeBase knowledge : relevantKnowledge) {
            Map<String, Object> knowledgeMsg = new HashMap<>();
            knowledgeMsg.put("role", "system");
            knowledgeMsg.put("content", "【知识库】" + knowledge.getTitle() + ": " + knowledge.getContent());
            messagesForAI.add(knowledgeMsg);
        }

        // 添加历史消息（限制数量，避免 token 超限）
        int maxHistoryMessages = 16; // 最多 16 条历史消息（8轮对话）
        int startIndex = Math.max(0, historyMessages.size() - maxHistoryMessages);

        for (int i = startIndex; i < historyMessages.size(); i++) {
            ChatMessage msg = historyMessages.get(i);
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("role", msg.getRole());
            msgMap.put("content", msg.getContent());
            messagesForAI.add(msgMap);
        }

        // 添加当前用户消息
        Map<String, Object> currentMsg = new HashMap<>();
        currentMsg.put("role", "user");
        currentMsg.put("content", request.getMessage());
        messagesForAI.add(currentMsg);

        logger.info("【聊天服务】发送消息列表到 AI，包含 {} 条消息（{} 条知识 + {} 条历史 + 1 条当前）",
            messagesForAI.size(), relevantKnowledge.size(), messagesForAI.size() - relevantKnowledge.size() - 1);

        // 保存用户消息到数据库（在调用 AI 之前）
        ChatMessage userMessage = ChatMessage.builder()
                .id(IdGenerator.generateId())
                .conversationId(conversationId)
                .userId(request.getUserId())
                .role("user")
                .content(request.getMessage())
                .status(1)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageMapper.insert(userMessage);
        logger.info("【聊天服务】用户消息已保存，消息ID: {}", userMessage.getId());

        // 缓存用户消息到 Redis
        chatCacheService.cacheMessage(conversationId, userMessage);
        logger.info("【聊天服务】用户消息已缓存到 Redis");

        // 调用阿里云百炼 API 获取AI回复（流式，包含历史消息）
        logger.info("【聊天服务】调用阿里云百炼 API（流式+历史），模型: qwen2.5-vl-72b-instruct");
        long startTime = System.currentTimeMillis();

        // 使用流式调用 - llmClient 会实时调用 chunkHandler
        String aiResponse = llmClient.sendMessageStreamWithHistory(messagesForAI, chunkHandler);

        long responseTime = System.currentTimeMillis() - startTime;
        logger.info("【聊天服务】AI回复生成成功（流式），响应时间: {}ms", responseTime);

        // 保存AI回复到数据库
        ChatMessage assistantMessage = ChatMessage.builder()
                .id(IdGenerator.generateId())
                .conversationId(conversationId)
                .userId(request.getUserId())
                .role("assistant")
                .content(aiResponse)
                .tokens(estimateTokens(aiResponse)) // 估算token数量
                .responseTime((int) responseTime)
                .status(1)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageMapper.insert(assistantMessage);
        logger.info("【聊天服务】AI回复已保存，消息ID: {}, Token数: {}", assistantMessage.getId(), assistantMessage.getTokens());

        // 缓存AI回复到 Redis
        chatCacheService.cacheMessage(conversationId, assistantMessage);
        logger.info("【聊天服务】AI回复已缓存到 Redis");

        // 缓存会话信息
        chatCacheService.cacheConversation(conversation);
        logger.info("【聊天服务】会话信息已缓存到 Redis");

        // 更新会话标题（如果是第一条消息）
        if (conversation.getTitle().equals("新对话")) {
            String title = request.getMessage().length() > 20
                ? request.getMessage().substring(0, 20)
                : request.getMessage();
            conversation.setTitle(title);
            conversationMapper.updateById(conversation);
            logger.info("【聊天服务】更新会话标题为: {}", title);
        }

        // 自动提取知识并存储到知识库
        try {
            extractKnowledge(request.getUserId(), request.getMessage(), aiResponse);
        } catch (Exception e) {
            logger.warn("【聊天服务】知识提取失败: {}", e.getMessage());
        }

        // 构建响应
        ChatResponse response = new ChatResponse();
        response.setConversationId(conversationId);
        response.setMessageId(assistantMessage.getId());
        response.setContent(aiResponse);
        response.setRole("assistant");
        response.setTokens(assistantMessage.getTokens());
        response.setResponseTime(assistantMessage.getResponseTime());
        response.setCreatedAt(assistantMessage.getCreatedAt());

        logger.info("【聊天服务】流式消息处理完成，会话ID: {}, 消息ID: {}", conversationId, assistantMessage.getId());
        return response;
    }

    @Override
    @Transactional
    public Conversation createConversation(Long userId, String title) {
        logger.info("【会话服务】创建新会话，用户ID: {}, 标题: {}", userId, title);

        Conversation conversation = Conversation.builder()
                .id(IdGenerator.generateId())
                .userId(userId)
                .title(title)
                .model("qwen2.5-vl-72b-instruct")
                .temperature(0.7)
                .contextWindow(2048)
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        conversationMapper.insert(conversation);

        logger.info("【会话服务】会话创建成功，会话ID: {}", conversation.getId());
        return conversation;
    }

    @Override
    public List<Conversation> getConversations(Long userId) {
        logger.info("【会话服务】获取用户 {} 的会话列表", userId);

        // 先从 Redis 获取
        List<Conversation> conversations = chatCacheService.getUserConversations(userId);

        if (conversations == null || conversations.isEmpty()) {
            logger.info("【会话服务】Redis 缓存未命中，从数据库查询");

            LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Conversation::getUserId, userId)
                   .eq(Conversation::getStatus, 1)
                   .orderByDesc(Conversation::getUpdatedAt);
            conversations = conversationMapper.selectList(wrapper);

            // 缓存到 Redis
            chatCacheService.cacheUserConversations(userId, conversations);
            logger.info("【会话服务】会话列表已缓存到 Redis，共 {} 个会话", conversations.size());
        } else {
            logger.info("【会话服务】从 Redis 缓存获取会话列表，共 {} 个会话", conversations.size());
        }

        return conversations;
    }

    @Override
    public List<ChatMessage> getMessages(Long conversationId) {
        logger.info("【消息服务】获取会话 {} 的消息列表", conversationId);

        // 先从 Redis 获取
        List<ChatMessage> messages = chatCacheService.getMessages(conversationId);

        if (messages == null || messages.isEmpty()) {
            logger.info("【消息服务】Redis 缓存未命中，从数据库查询");

            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getConversationId, conversationId)
                   .eq(ChatMessage::getStatus, 1)
                   .orderByAsc(ChatMessage::getCreatedAt);
            messages = chatMessageMapper.selectList(wrapper);

            // 缓存到 Redis
            messages.forEach(msg -> chatCacheService.cacheMessage(conversationId, msg));
            logger.info("【消息服务】消息列表已缓存到 Redis，共 {} 条消息", messages.size());
        } else {
            logger.info("【消息服务】从 Redis 缓存获取消息列表，共 {} 条消息", messages.size());
        }

        return messages;
    }

    @Override
    @Transactional
    public boolean deleteConversation(Long conversationId) {
        logger.info("【会话服务】删除会话 {}", conversationId);

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setStatus(0);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
            logger.info("【会话服务】会话 {} 删除成功", conversationId);
            return true;
        } else {
            logger.warn("【会话服务】会话 {} 不存在", conversationId);
            return false;
        }
    }

    /**
     * 估算token数量（简单估算：中文字符约2个token，英文字符约0.3个token）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int chineseChars = 0;
        int englishChars = 0;
        for (char c : text.toCharArray()) {
            if (c >= '一' && c <= '鿿') {
                chineseChars++;
            } else if (Character.isLetterOrDigit(c)) {
                englishChars++;
            }
        }
        // 中文字符约2个token，英文字符约0.3个token
        return (int) (chineseChars * 2 + englishChars * 0.3);
    }

    /**
     * 自动提取知识并存储到知识库
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @param aiResponse AI回复
     */
    private void extractKnowledge(Long userId, String userMessage, String aiResponse) {
        logger.info("【聊天服务】开始提取知识，用户ID: {}", userId);

        // 规则1: 用户自我介绍
        if (userMessage.contains("我叫") || userMessage.contains("我是")) {
            String userName = extractName(userMessage);
            if (userName != null) {
                KnowledgeBase knowledge = KnowledgeBase.builder()
                        .userId(userId)
                        .title("用户信息: " + userName)
                        .content("用户姓名: " + userName + "。用户自我介绍: " + userMessage)
                        .category("user_info")
                        .tags("用户信息,自我介绍")
                        .build();
                knowledgeBaseService.addKnowledge(knowledge);
                logger.info("【聊天服务】提取用户信息成功: {}", userName);
            }
        }

        // 规则2: 重要事实（包含"记住"、"记得"等关键词）
        if (userMessage.contains("记住") || userMessage.contains("记得") || userMessage.contains("重要")) {
            KnowledgeBase knowledge = KnowledgeBase.builder()
                    .userId(userId)
                    .title("重要信息: " + userMessage.substring(0, Math.min(20, userMessage.length())))
                    .content(userMessage)
                    .category("important_info")
                    .tags("重要信息,记忆")
                    .build();
            knowledgeBaseService.addKnowledge(knowledge);
            logger.info("【聊天服务】提取重要信息成功");
        }

        // 规则3: 用户偏好（包含"喜欢"、"偏好"等关键词）
        if (userMessage.contains("喜欢") || userMessage.contains("偏好") || userMessage.contains("爱好")) {
            KnowledgeBase knowledge = KnowledgeBase.builder()
                    .userId(userId)
                    .title("用户偏好: " + userMessage.substring(0, Math.min(20, userMessage.length())))
                    .content(userMessage)
                    .category("user_preference")
                    .tags("偏好,爱好")
                    .build();
            knowledgeBaseService.addKnowledge(knowledge);
            logger.info("【聊天服务】提取用户偏好成功");
        }

        logger.info("【聊天服务】知识提取完成");
    }

    /**
     * 从用户消息中提取姓名
     * @param message 用户消息
     * @return 提取的姓名，如果没有则返回null
     */
    private String extractName(String message) {
        // 匹配"我叫XXX"或"我是XXX"的模式
        String[] patterns = {
            "我叫([\\u4e00-\\u9fa5a-zA-Z]+)",
            "我是([\\u4e00-\\u9fa5a-zA-Z]+)"
        };

        for (String pattern : patterns) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /**
     * RAG检索：从知识库检索相关知识
     * @param userId 用户ID
     * @param query 查询文本
     * @return 相关知识列表
     */
    private List<KnowledgeBase> searchRelevantKnowledge(Long userId, String query) {
        try {
            // 1. 关键词搜索
            List<KnowledgeBase> keywordResults = knowledgeBaseService.searchByKeyword(userId, query);

            // 2. 向量相似性搜索（如果可用）
            List<KnowledgeBase> vectorResults = knowledgeBaseService.searchByVector(userId, query);

            // 3. 合并并去重
            return mergeAndDeduplicate(keywordResults, vectorResults);
        } catch (Exception e) {
            logger.warn("【聊天服务】RAG检索失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 合并并去重知识库搜索结果
     */
    private List<KnowledgeBase> mergeAndDeduplicate(List<KnowledgeBase> results1, List<KnowledgeBase> results2) {
        Map<Long, KnowledgeBase> merged = new HashMap<>();
        for (KnowledgeBase kb : results1) {
            merged.put(kb.getId(), kb);
        }
        for (KnowledgeBase kb : results2) {
            merged.putIfAbsent(kb.getId(), kb);
        }
        return new ArrayList<>(merged.values());
    }
}
