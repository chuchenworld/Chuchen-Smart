package com.example.chuchensmart.controller;

import com.example.chuchensmart.common.R;
import com.example.chuchensmart.dto.ChatRequest;
import com.example.chuchensmart.dto.ChatResponse;
import com.example.chuchensmart.entity.ChatMessage;
import com.example.chuchensmart.entity.Conversation;
import com.example.chuchensmart.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天控制器
 * @author 小李
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 发送消息并获取AI回复（非流式）
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/send")
    public R<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        logger.info("【普通对话】用户 {} 发送消息: {}", request.getUserId(), request.getMessage());

        try {
            ChatResponse response = chatService.sendMessage(request);
            logger.info("【普通对话】用户 {} 收到AI回复: {}", request.getUserId(), response.getContent());
            return R.success(response);
        } catch (Exception e) {
            logger.error("【普通对话】用户 {} 发送消息失败: {}", request.getUserId(), e.getMessage(), e);
            return R.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息并获取AI回复（流式）
     * @param request 聊天请求
     * @return SSE流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@RequestBody ChatRequest request) {
        logger.info("【流式对话】用户 {} 发送流式消息: {}", request.getUserId(), request.getMessage());

        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时

        executor.execute(() -> {
            try {
                // 创建临时响应对象用于返回
                final ChatResponse[] finalResponse = new ChatResponse[1];
                final Long[] conversationIdHolder = new Long[1];

                // 使用流式调用
                ChatResponse response = chatService.sendMessageStream(request, chunk -> {
                    try {
                        // 发送数据块 - LlmClient 已经发送了 JSON 格式的数据
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (Exception e) {
                        logger.error("【流式对话】发送数据块失败: {}", e.getMessage());
                    }
                });

                finalResponse[0] = response;
                conversationIdHolder[0] = response.getConversationId();

                // 发送完成标记，包含 conversationId
                Map<String, Object> doneData = new HashMap<>();
                doneData.put("done", true);
                doneData.put("conversationId", conversationIdHolder[0]);
                String doneJson = objectMapper.writeValueAsString(doneData);
                emitter.send(SseEmitter.event().data(doneJson));

                emitter.complete();
                logger.info("【流式对话】用户 {} 流式消息处理完成", request.getUserId());

            } catch (Exception e) {
                logger.error("【流式对话】用户 {} 流式消息处理失败: {}", request.getUserId(), e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理消息时出现错误: " + e.getMessage()));
                } catch (IOException ioException) {
                    logger.error("【流式对话】发送错误事件失败: {}", ioException.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 创建新会话
     * @param userId 用户ID
     * @param title 会话标题（可选）
     * @return 会话信息
     */
    @PostMapping("/conversation")
    public R<Conversation> createConversation(
            @RequestParam Long userId,
            @RequestParam(required = false) String title) {
        logger.info("【会话管理】用户 {} 请求创建新会话，标题: {}", userId, title);

        try {
            String conversationTitle = title != null && !title.isEmpty() ? title : "新对话";
            Conversation conversation = chatService.createConversation(userId, conversationTitle);
            logger.info("【会话管理】用户 {} 创建会话成功，会话ID: {}", userId, conversation.getId());
            return R.success("会话创建成功", conversation);
        } catch (Exception e) {
            logger.error("【会话管理】用户 {} 创建会话失败: {}", userId, e.getMessage(), e);
            return R.error("会话创建失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    @GetMapping("/conversations")
    public R<List<Conversation>> getConversations(@RequestParam Long userId) {
        logger.info("【会话管理】用户 {} 请求获取会话列表", userId);

        try {
            List<Conversation> conversations = chatService.getConversations(userId);
            logger.info("【会话管理】用户 {} 获取会话列表成功，共 {} 个会话", userId, conversations.size());
            return R.success(conversations);
        } catch (Exception e) {
            logger.error("【会话管理】用户 {} 获取会话列表失败: {}", userId, e.getMessage(), e);
            return R.error("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话消息列表
     * @param conversationId 会话ID（支持字符串格式，避免前端精度丢失问题）
     * @return 消息列表
     */
    @GetMapping("/messages")
    public R<List<ChatMessage>> getMessages(@RequestParam String conversationId) {
        logger.info("【消息管理】请求获取会话 {} 的消息列表", conversationId);

        try {
            // 将字符串转换为 Long
            Long convId = Long.parseLong(conversationId);
            List<ChatMessage> messages = chatService.getMessages(convId);
            logger.info("【消息管理】获取会话 {} 的消息列表成功，共 {} 条消息", conversationId, messages.size());
            return R.success(messages);
        } catch (NumberFormatException e) {
            logger.error("【消息管理】会话ID格式错误: {}", conversationId);
            return R.error("会话ID格式错误");
        } catch (Exception e) {
            logger.error("【消息管理】获取会话 {} 的消息列表失败: {}", conversationId, e.getMessage(), e);
            return R.error("获取消息列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     * @param conversationId 会话ID（支持字符串格式，避免前端精度丢失问题）
     * @return 删除结果
     */
    @DeleteMapping("/conversation/{conversationId}")
    public R<String> deleteConversation(@PathVariable String conversationId) {
        logger.info("【会话管理】请求删除会话 {}", conversationId);

        try {
            // 将字符串转换为 Long
            Long convId = Long.parseLong(conversationId);
            boolean success = chatService.deleteConversation(convId);
            if (success) {
                logger.info("【会话管理】会话 {} 删除成功", conversationId);
                return R.success("会话删除成功");
            } else {
                logger.warn("【会话管理】会话 {} 删除失败", conversationId);
                return R.error("会话删除失败");
            }
        } catch (NumberFormatException e) {
            logger.error("【会话管理】会话ID格式错误: {}", conversationId);
            return R.error("会话ID格式错误");
        } catch (Exception e) {
            logger.error("【会话管理】删除会话 {} 失败: {}", conversationId, e.getMessage(), e);
            return R.error("删除会话失败: " + e.getMessage());
        }
    }
}
