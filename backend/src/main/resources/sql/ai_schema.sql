-- AI 模块数据库表设计
-- 用于提升 AI 功能效率的表结构

-- 1. 对话会话表 (管理对话上下文)
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) COMMENT '会话标题',
    model VARCHAR(100) DEFAULT 'llama2:7b' COMMENT '使用的AI模型',
    temperature DECIMAL(3,2) DEFAULT 0.9 COMMENT '温度参数',
    context_window INT DEFAULT 4096 COMMENT '上下文窗口大小',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-活跃, 0-归档',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) COMMENT='对话会话表';

-- 2. 聊天消息表 (存储对话历史)
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role ENUM('user', 'assistant', 'system') NOT NULL COMMENT '消息角色',
    content TEXT NOT NULL COMMENT '消息内容',
    tokens INT DEFAULT 0 COMMENT '消耗的token数量',
    response_time INT DEFAULT 0 COMMENT '响应时间(毫秒)',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FULLTEXT idx_content (content)
) COMMENT='聊天消息表';

-- 3. 知识库表 (存储AI需要检索的知识)
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY COMMENT '知识ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) NOT NULL COMMENT '知识标题',
    content TEXT NOT NULL COMMENT '知识内容',
    category VARCHAR(100) COMMENT '分类',
    tags VARCHAR(500) COMMENT '标签(逗号分隔)',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_tags (tags(100)),
    FULLTEXT idx_content (content)
) COMMENT='知识库表';

-- 4. 向量嵌入表 (用于相似性搜索和RAG)
CREATE TABLE IF NOT EXISTS vector_embedding (
    id BIGINT PRIMARY KEY COMMENT '向量ID',
    source_id BIGINT NOT NULL COMMENT '源数据ID',
    source_type ENUM('knowledge', 'message', 'document') NOT NULL COMMENT '源数据类型',
    embedding VECTOR(1536) NOT NULL COMMENT '向量嵌入(1536维)',
    content_hash VARCHAR(64) COMMENT '内容哈希(用于去重)',
    metadata JSON COMMENT '元数据(JSON格式)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_source (source_type, source_id),
    INDEX idx_hash (content_hash),
    VECTOR INDEX idx_embedding (embedding)
) COMMENT='向量嵌入表';

-- 5. 提示模板表 (存储常用的提示模板)
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY COMMENT '模板ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description VARCHAR(500) COMMENT '模板描述',
    template TEXT NOT NULL COMMENT '模板内容',
    variables VARCHAR(500) COMMENT '变量列表(逗号分隔)',
    category VARCHAR(100) COMMENT '分类',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认模板',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_name (name)
) COMMENT='提示模板表';

-- 6. AI 会话状态表 (管理AI对话状态)
CREATE TABLE IF NOT EXISTS ai_session (
    id BIGINT PRIMARY KEY COMMENT '会话状态ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    session_key VARCHAR(100) NOT NULL COMMENT '会话键',
    session_value TEXT COMMENT '会话值',
    expire_time DATETIME COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_session_key (session_key),
    INDEX idx_expire_time (expire_time)
) COMMENT='AI 会话状态表';

-- 7. AI 性能统计表 (用于监控和优化)
CREATE TABLE IF NOT EXISTS ai_performance (
    id BIGINT PRIMARY KEY COMMENT '统计ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    model VARCHAR(100) NOT NULL COMMENT 'AI模型',
    request_count INT DEFAULT 0 COMMENT '请求次数',
    total_tokens INT DEFAULT 0 COMMENT '总token数',
    avg_response_time INT DEFAULT 0 COMMENT '平均响应时间(毫秒)',
    success_count INT DEFAULT 0 COMMENT '成功次数',
    error_count INT DEFAULT 0 COMMENT '错误次数',
    statistics_date DATE NOT NULL COMMENT '统计日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_model (model),
    INDEX idx_statistics_date (statistics_date),
    UNIQUE KEY uk_user_model_date (user_id, model, statistics_date)
) COMMENT='AI 性能统计表';

-- 8. 知识库标签表 (用于标签管理)
CREATE TABLE IF NOT EXISTS knowledge_tag (
    id BIGINT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    color VARCHAR(20) COMMENT '标签颜色',
    count INT DEFAULT 0 COMMENT '使用次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_name (name)
) COMMENT='知识库标签表';

-- 9. 知识库关联表 (知识与标签的多对多关系)
CREATE TABLE IF NOT EXISTS knowledge_tag_relation (
    knowledge_id BIGINT NOT NULL COMMENT '知识ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (knowledge_id, tag_id),
    INDEX idx_tag_id (tag_id)
) COMMENT='知识库标签关联表';

-- 插入默认提示模板
INSERT INTO prompt_template (id, name, description, template, variables, category, is_default) VALUES
(1, '通用对话', '通用对话模板', '你是一个有用的AI助手，请用中文回答用户的问题。用户的问题是：{{question}}', 'question', 'general', TRUE),
(2, '代码解释', '代码解释模板', '请解释以下代码的功能和逻辑：\n{{code}}\n请用中文详细说明。', 'code', 'programming', FALSE),
(3, '文档总结', '文档总结模板', '请总结以下文档的主要内容：\n{{document}}\n请用中文输出要点。', 'document', 'writing', FALSE),
(4, '翻译助手', '翻译助手模板', '请将以下文本从{{source_lang}}翻译成{{target_lang}}：\n{{text}}', 'source_lang,target_lang,text', 'translation', FALSE);
