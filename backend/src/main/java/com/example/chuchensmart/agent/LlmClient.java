package com.example.chuchensmart.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 阿里云百炼 LLM 客户端
 * @author 小李
 */
@Component
public class LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${spring.ai.dashscope.chat.options.model:qwen2.5-vl-72b-instruct}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发送消息并获取AI回复（非流式）
     * @param message 用户消息
     * @return AI回复内容
     */
    public String sendMessage(String message) {
        logger.info("【LLM客户端】调用阿里云百炼 API，模型: {}", model);
        logger.info("【LLM客户端】用户消息: {}", message);

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/chat/completions";

            logger.info("【LLM客户端】发送请求到: {}", url);
            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.postForEntity(
                url, entity, String.class
            );

            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("【LLM客户端】API响应时间: {}ms", responseTime);

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode messageNode = choices.get(0).path("message");
                    String content = messageNode.path("content").asText();
                    logger.info("【LLM客户端】AI回复内容长度: {} 字符", content.length());
                    return content;
                }
            }

            logger.warn("【LLM客户端】API返回非OK状态: {}", response.getStatusCode());
            return "抱歉，无法获取AI回复。";

        } catch (Exception e) {
            logger.error("【LLM客户端】调用阿里云API失败: {}", e.getMessage(), e);
            return "抱歉，调用AI服务时出现错误。";
        }
    }

    /**
     * 发送消息并获取AI回复（流式）
     * @param message 用户消息
     * @param chunkHandler 数据块处理器
     * @return 完整回复内容
     */
    public String sendMessageStream(String message, Consumer<String> chunkHandler) {
        logger.info("【LLM客户端】调用阿里云百炼 API(流式)，模型: {}", model);
        logger.info("【LLM客户端】用户消息: {}", message);

        StringBuilder fullContent = new StringBuilder();

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("stream", true);

            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            // 转换为 JSON
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // 创建 HTTP 连接
            String url = baseUrl + "/chat/completions";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            logger.info("【LLM客户端】发送流式请求到: {}", url);
            long startTime = System.currentTimeMillis();

            // 发送请求体
            try (var outputStream = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }

            // 读取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 处理 "data: " 和 "data:" 两种格式
                        String data = null;
                        if (line.startsWith("data: ")) {
                            data = line.substring(6);
                        } else if (line.startsWith("data:")) {
                            data = line.substring(5);
                        }

                        if (data != null) {
                            if (data.equals("[DONE]")) {
                                break;
                            }
                            try {
                                JsonNode chunk = objectMapper.readTree(data);
                                JsonNode choices = chunk.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    String content = delta.path("content").asText();
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        // 调用处理器，发送 JSON 格式的数据
                                        Map<String, String> dataMap = new HashMap<>();
                                        dataMap.put("content", content);
                                        String jsonData = objectMapper.writeValueAsString(dataMap);
                                        logger.debug("【LLM客户端】发送数据块: {}", jsonData.substring(0, Math.min(100, jsonData.length())));
                                        chunkHandler.accept(jsonData);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("解析流式数据块失败: {}", e.getMessage());
                            }
                        }
                    }
                }

                long responseTime = System.currentTimeMillis() - startTime;
                logger.info("【LLM客户端】流式API响应时间: {}ms", responseTime);
                logger.info("【LLM客户端】AI回复内容长度: {} 字符", fullContent.length());
            } else {
                logger.warn("【LLM客户端】API返回非OK状态: {}", responseCode);
                return "抱歉，无法获取AI回复。";
            }

            connection.disconnect();

        } catch (Exception e) {
            logger.error("【LLM客户端】调用阿里云流式API失败: {}", e.getMessage(), e);
            return "抱歉，调用AI服务时出现错误。";
        }

        return fullContent.toString();
    }

    /**
     * 发送消息列表（包含对话历史）
     * @param messages 消息列表 [{role: "user/assistant", content: "..."}, ...]
     * @return AI回复内容
     */
    public String sendMessageWithHistory(List<Map<String, Object>> messages) {
        logger.info("【LLM客户端】调用阿里云百炼 API（带历史），模型: {}", model);
        logger.info("【LLM客户端】发送消息列表，包含 {} 条消息", messages.size());

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/chat/completions";

            logger.info("【LLM客户端】发送请求到: {}", url);
            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.postForEntity(
                url, entity, String.class
            );

            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("【LLM客户端】API响应时间: {}ms", responseTime);

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode messageNode = choices.get(0).path("message");
                    String content = messageNode.path("content").asText();
                    logger.info("【LLM客户端】AI回复内容长度: {} 字符", content.length());
                    return content;
                }
            }

            logger.warn("【LLM客户端】API返回非OK状态: {}", response.getStatusCode());
            return "抱歉，无法获取AI回复。";

        } catch (Exception e) {
            logger.error("【LLM客户端】调用阿里云API失败: {}", e.getMessage(), e);
            return "抱歉，调用AI服务时出现错误。";
        }
    }

    /**
     * 发送消息列表（流式，包含对话历史）
     * @param messages 消息列表 [{role: "user/assistant", content: "..."}, ...]
     * @param chunkHandler 数据块处理器
     * @return 完整回复内容
     */
    public String sendMessageStreamWithHistory(List<Map<String, Object>> messages, Consumer<String> chunkHandler) {
        logger.info("【LLM客户端】调用阿里云百炼 API（流式+历史），模型: {}", model);
        logger.info("【LLM客户端】发送消息列表，包含 {} 条消息", messages.size());

        StringBuilder fullContent = new StringBuilder();

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("stream", true);
            requestBody.put("messages", messages);

            // 转换为 JSON
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // 创建 HTTP 连接
            String url = baseUrl + "/chat/completions";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            logger.info("【LLM客户端】发送流式请求到: {}", url);
            long startTime = System.currentTimeMillis();

            // 发送请求体
            try (var outputStream = connection.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }

            // 读取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 处理 "data: " 和 "data:" 两种格式
                        String data = null;
                        if (line.startsWith("data: ")) {
                            data = line.substring(6);
                        } else if (line.startsWith("data:")) {
                            data = line.substring(5);
                        }

                        if (data != null) {
                            if (data.equals("[DONE]")) {
                                break;
                            }
                            try {
                                JsonNode chunk = objectMapper.readTree(data);
                                JsonNode choices = chunk.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    String content = delta.path("content").asText();
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        // 调用处理器，发送 JSON 格式的数据
                                        Map<String, String> dataMap = new HashMap<>();
                                        dataMap.put("content", content);
                                        String jsonData = objectMapper.writeValueAsString(dataMap);
                                        logger.debug("【LLM客户端】发送数据块: {}", jsonData.substring(0, Math.min(100, jsonData.length())));
                                        chunkHandler.accept(jsonData);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("解析流式数据块失败: {}", e.getMessage());
                            }
                        }
                    }
                }

                long responseTime = System.currentTimeMillis() - startTime;
                logger.info("【LLM客户端】流式API响应时间: {}ms", responseTime);
                logger.info("【LLM客户端】AI回复内容长度: {} 字符", fullContent.length());
            } else {
                logger.warn("【LLM客户端】API返回非OK状态: {}", responseCode);
                return "抱歉，无法获取AI回复。";
            }

            connection.disconnect();

        } catch (Exception e) {
            logger.error("【LLM客户端】调用阿里云流式API失败: {}", e.getMessage(), e);
            return "抱歉，调用AI服务时出现错误。";
        }

        return fullContent.toString();
    }

    /**
     * 生成文本的向量嵌入（用于RAG和相似性搜索）
     * @param text 要生成嵌入的文本
     * @return 向量嵌入（1536维）
     */
    public String generateEmbedding(String text) {
        logger.info("【LLM客户端】调用阿里云百炼嵌入API，生成文本向量");
        logger.info("【LLM客户端】文本内容: {}", text);

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-embedding-v2");
            requestBody.put("input", text);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

            logger.info("【LLM客户端】发送嵌入请求到: {}", url);
            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.postForEntity(
                url, entity, String.class
            );

            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("【LLM客户端】嵌入API响应时间: {}ms", responseTime);

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddings = root.path("output").path("embeddings");
                if (embeddings.isArray() && embeddings.size() > 0) {
                    JsonNode embedding = embeddings.get(0);
                    String embeddingText = embedding.path("embedding").asText();
                    logger.info("【LLM客户端】生成向量嵌入成功，维度: {}", embeddingText.length());
                    return embeddingText;
                }
            }

            logger.warn("【LLM客户端】嵌入API返回非OK状态: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            logger.error("【LLM客户端】调用嵌入API失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
