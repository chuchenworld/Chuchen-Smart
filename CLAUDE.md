# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个全栈 AI 聊天应用，包含 Spring Boot 后端和 Vue 3 前端。

**技术栈：**
- **后端：** Java 21, Spring Boot 3.5.14, MyBatis, MyBatis-Plus, MySQL
- **前端：** Vue 3, Vite, Element Plus, Tailwind CSS

## 目录结构

```
Chuchen Smart/
├── backend/        # Spring Boot 后端
├── frontend/       # Vue 3 前端
└── CLAUDE.md       # 本文件
```

## 常用命令

### 后端 (Maven)
```bash
cd backend
# 构建和打包
mvn clean install

# 运行开发服务器 (默认使用 application-dev.yml)
mvn spring-boot:run

# 运行测试
mvn test

# 打包为 JAR
mvn package
```

### 前端 (Vite/npm)
```bash
cd frontend
# 启动开发服务器 (端口 3000, 代理 /api 到后端:8080)
npm run dev

# 构建生产版本 (输出到 dist/)
npm run build

# 预览构建文件
npm run preview
```

### 开发环境
- **后端端口：** 8080
- **前端端口：** 3000
- **数据库：** MySQL at `localhost:3306/chuchensmart`
- **激活的 profile：** `dev` (默认)

## 后端架构

### 包结构
```
backend/src/main/java/com/example/chuchensmart/
├── agent/          # LLM 集成 (LlmClient, PromptBuilder, TollService)
├── common/         # 工具类 (Constant, Exception, R, Utils, SnowflakeIdGenerator)
├── config/         # Spring 配置 (CorsConfig, RestTemplateConfig, SwaggerConfig)
├── controller/     # REST 控制器 (ChatController, UserController)
├── dto/            # 数据传输对象 (ChatRequest, ChatResponse, UserDto)
├── entity/         # 数据库实体 (ChatMessage, User, Conversation, etc.)
├── mapper/         # MyBatis 映射器 (ChatMessageMapper, UserMapper)
├── service/        # 业务逻辑接口 (ChatService, UserService)
│   └── impl/       # 服务实现 (ChatServiceImpl)
├── utils/          # 工具类 (IdGenerator)
└── vo/             # 视图对象 (UserVo)
```

### 关键组件
- **ChatController：** 聊天功能的 REST API 端点（已实现）
  - `POST /api/chat/send` - 发送消息并获取AI回复
  - `POST /api/chat/conversation` - 创建新会话
  - `GET /api/chat/conversations` - 获取用户会话列表
  - `GET /api/chat/messages` - 获取会话消息列表
  - `DELETE /api/chat/conversation/{id}` - 删除会话
- **UserController：** 用户管理的 REST API 端点（已实现）
  - `GET /api/user/{id}` - 根据ID查询用户
  - `POST /api/user` - 创建用户
- **LlmClient：** 阿里云百炼 LLM 集成客户端（已实现）
  - 使用 qwen2.5-vl-72b-instruct 模型
  - 兼容 OpenAI API 协议
- **R：** 通用响应包装类
- **MyBatis/MyBatis-Plus：** 通过映射器进行数据库访问

### 配置文件
- **application.yml：** 基础配置，包含 MyBatis 设置，端口 8080
- **application-dev.yml：** 开发配置 (MySQL 连接, 阿里云百炼配置)
  - `spring.ai.dashscope.api-key`：阿里云百炼 API Key
  - `spring.ai.dashscope.base-url`：阿里云百炼 API 地址
  - `spring.ai.dashscope.chat.options.model`：模型名称 (qwen2.5-vl-72b-instruct)
  - `spring.ai.dashscope.chat.options.temperature`：温度参数 (0.7)
  - `spring.ai.dashscope.chat.options.max-tokens`：最大 token 数 (2048)
- **application-prod.yml：** 生产配置

### 数据库表结构
后端定义了以下数据库实体：
- `Conversation` - 对话会话表
- `ChatMessage` - 聊天消息表
- `KnowledgeBase` - 知识库表
- `VectorEmbedding` - 向量嵌入表
- `PromptTemplate` - 提示模板表
- `AiPerformance` - AI 性能统计表
- `User` - 用户表

完整的数据库表定义在 `backend/src/main/resources/sql/ai_schema.sql`

## 前端架构

### 目录结构
```
frontend/src/
├── assets/         # 静态资源 (图片、图标)
├── components/     # 可复用的 Vue 组件
│   └── ChatWindow/ # AI 聊天悬浮窗组件
│       ├── ChatButton.vue   # 悬浮按钮组件
│       └── ChatWindow.vue   # AI 对话悬浮窗主组件
├── App.vue         # 根组件
├── main.js         # 入口文件
└── style.css       # 全局样式
```

### 关键组件
- **ChatWindow.vue：** AI 对话悬浮窗主组件
  - 实现了拖拽功能、消息列表、输入区域
  - 当前使用模拟回复，未连接后端 API
- **ChatButton.vue：** 悬浮按钮组件

### 配置
- **vite.config.js：** 开发服务器端口 3000, 代理 /api 到后端:8080
- **tailwind.config.js：** Tailwind CSS 配置
- **postcss.config.js：** PostCSS 配置

## 数据库配置
- **URL：** `jdbc:mysql://localhost:3306/chuchensmart?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`
- **用户名/密码：** 在 application-dev.yml 中配置（默认 root/root）

## 测试
- **后端：** JUnit 5 via `spring-boot-starter-test`
- **前端：** 未配置测试框架

## 依赖

### 后端
- Spring Boot 3.5.14
- MyBatis Spring Boot Starter 3.0.5
- MyBatis-Plus Spring Boot3 Starter 3.5.9
- MySQL Connector J
- Lombok
- Spring Boot DevTools
- SpringDoc OpenAPI (Swagger UI) 2.8.9
- Spring Boot Validation
- 阿里云百炼 API（DashScope）

### 前端
- Vue 3.5.34
- Vite 8.0.12
- Element Plus 2.14.0
- Tailwind CSS 4.3.0
- @element-plus/icons-vue 2.3.2

## 当前状态

### 后端
- 项目骨架已搭建完成，业务逻辑已部分实现
- **ChatController** - 已补全，包含以下端点：
  - `POST /api/chat/send` - 发送消息并获取AI回复
  - `POST /api/chat/conversation` - 创建新会话
  - `GET /api/chat/conversations` - 获取用户会话列表
  - `GET /api/chat/messages` - 获取会话消息列表
  - `DELETE /api/chat/conversation/{id}` - 删除会话
- **UserController** - 已补全，包含以下端点：
  - `GET /api/user/{id}` - 根据ID查询用户
  - `POST /api/user` - 创建用户
- **ChatService** - 已实现，包含消息发送、会话管理等功能
- **UserService** - 已实现，包含用户查询和创建功能
- **DTO类** - 已创建 ChatRequest 和 ChatResponse
- **LlmClient** - 已实现，集成阿里云百炼 API，使用 qwen2.5-vl-72b-instruct 模型
- 数据库表结构已定义，MyBatis 映射器已存在

### 前端
- ChatWindow 组件已实现基本的聊天界面
- **已连接后端 API**，不再使用模拟回复
- API 服务文件：`frontend/src/api/chat.js`
  - `sendMessage(request)` - 发送消息并获取AI回复
  - `createConversation(userId, title)` - 创建新会话
  - `getConversations(userId)` - 获取用户会话列表
  - `getMessages(conversationId)` - 获取会话消息列表
  - `deleteConversation(conversationId)` - 删除会话
- 已安装 axios 依赖用于 HTTP 请求
- Vite 代理配置已正确设置（/api -> http://localhost:8080）
