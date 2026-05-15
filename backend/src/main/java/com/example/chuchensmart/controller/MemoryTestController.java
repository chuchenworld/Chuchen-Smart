package com.example.chuchensmart.controller;

import com.example.chuchensmart.common.R;
import com.example.chuchensmart.dto.ChatRequest;
import com.example.chuchensmart.dto.ChatResponse;
import com.example.chuchensmart.entity.ChatMessage;
import com.example.chuchensmart.entity.Conversation;
import com.example.chuchensmart.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话记忆功能测试控制器
 * 用于验证 AI 是否能够记住对话历史
 */
@RestController
@RequestMapping("/api/memory/test")
public class MemoryTestController {

    private static final Logger logger = LoggerFactory.getLogger(MemoryTestController.class);

    @Autowired
    private ChatService chatService;

    /**
     * 测试对话记忆功能
     * 步骤：
     * 1. 发送第一条消息：你好，我叫小明
     * 2. 发送第二条消息：我叫什么名字？
     * 3. 验证 AI 是否记住第一条消息
     */
    @PostMapping("/test-memory")
    public R<String> testMemory(@RequestParam Long userId) {
        try {
            logger.info("【记忆测试】开始测试对话记忆功能，用户ID: {}", userId);

            // 步骤1：创建新会话
            Conversation conversation = chatService.createConversation(userId, "记忆测试");
            Long conversationId = conversation.getId();
            logger.info("【记忆测试】创建会话成功，会话ID: {}", conversationId);

            // 步骤2：发送第一条消息
            ChatRequest request1 = new ChatRequest();
            request1.setUserId(userId);
            request1.setConversationId(conversationId);
            request1.setMessage("你好，我叫小明");

            logger.info("【记忆测试】发送第一条消息: {}", request1.getMessage());
            ChatResponse response1 = chatService.sendMessage(request1);
            logger.info("【记忆测试】AI回复1: {}", response1.getContent());

            // 等待一小段时间
            Thread.sleep(1000);

            // 步骤3：发送第二条消息（询问名字）
            ChatRequest request2 = new ChatRequest();
            request2.setUserId(userId);
            request2.setConversationId(conversationId);
            request2.setMessage("我叫什么名字？");

            logger.info("【记忆测试】发送第二条消息: {}", request2.getMessage());
            ChatResponse response2 = chatService.sendMessage(request2);
            logger.info("【记忆测试】AI回复2: {}", response2.getContent());

            // 步骤4：验证 AI 是否记住名字
            String aiResponse = response2.getContent();
            boolean remembered = aiResponse.contains("小明") || aiResponse.contains("小明");

            // 获取会话消息列表，验证历史消息
            List<ChatMessage> messages = chatService.getMessages(conversationId);
            logger.info("【记忆测试】会话消息列表，共 {} 条消息", messages.size());

            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("测试结果:\n");
            result.append("1. 会话ID: ").append(conversationId).append("\n");
            result.append("2. 消息总数: ").append(messages.size()).append("\n");
            result.append("3. AI是否记住名字: ").append(remembered ? "✅ 是" : "❌ 否").append("\n");
            result.append("4. AI回复: ").append(aiResponse).append("\n");
            result.append("\n会话历史:\n");
            for (ChatMessage msg : messages) {
                result.append("  - ").append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }

            logger.info("【记忆测试】测试完成，结果: {}", remembered ? "通过" : "失败");

            return R.success(result.toString());

        } catch (Exception e) {
            logger.error("【记忆测试】测试失败: {}", e.getMessage(), e);
            return R.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话消息列表（用于调试）
     */
    @GetMapping("/messages")
    public R<List<ChatMessage>> getMessages(@RequestParam Long conversationId) {
        try {
            List<ChatMessage> messages = chatService.getMessages(conversationId);
            return R.success(messages);
        } catch (Exception e) {
            logger.error("获取消息列表失败: {}", e.getMessage());
            return R.error("获取消息列表失败: " + e.getMessage());
        }
    }
}
