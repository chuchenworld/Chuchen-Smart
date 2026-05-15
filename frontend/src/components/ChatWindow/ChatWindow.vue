<template>
  <div class="chat-window-container">
    <!-- 悬浮按钮 -->
    <ChatButton
      v-if="!isOpen"
      @click="toggleChat"
    />

    <!-- 对话窗口 -->
    <transition name="slide-fade">
      <div
        v-if="isOpen"
        class="chat-panel"
        :style="panelStyle"
        @mousedown="startDrag"
      >
        <!-- 侧边栏 - 会话列表 -->
        <div class="sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
          <div class="sidebar-header">
            <h3 v-if="!sidebarCollapsed">会话</h3>
            <button class="toggle-sidebar-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开' : '折叠'">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path v-if="sidebarCollapsed" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
                <path v-else d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
              </svg>
            </button>
          </div>

          <div class="sidebar-content" v-if="!sidebarCollapsed">
            <!-- 创建新会话按钮 -->
            <button class="new-conversation-btn" @click="createNewConversation">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
              </svg>
              <span>新会话</span>
            </button>

            <!-- 会话列表 -->
            <div class="conversation-list">
              <div
                v-for="conv in conversations"
                :key="conv.id"
                :class="['conversation-item', { active: String(conv.id) === conversationId }]"
                @click="selectConversation(conv)"
              >
                <div class="conversation-info">
                  <div class="conversation-title">{{ conv.title }}</div>
                  <div class="conversation-time">{{ formatConversationTime(conv.updatedAt) }}</div>
                </div>
                <button
                  class="delete-conversation-btn"
                  @click.stop="deleteConversation(conv.id)"
                  title="删除会话"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 主聊天区域 -->
        <div class="chat-main">
          <!-- 顶部标题栏 -->
          <div class="chat-header" @mousedown="startDrag">
            <div class="header-left">
              <div class="header-avatar">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </div>
              <span class="chat-title">{{ currentConversationTitle }}</span>
            </div>
            <div class="header-right">
              <button class="header-btn minimize" @click="toggleChat" title="最小化">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                  <rect x="1" y="5" width="10" height="2" rx="1"/>
                </svg>
              </button>
              <button class="header-btn close" @click="closeChat" title="关闭">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                  <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="chat-messages" ref="messagesContainer">
            <div
              v-for="(msg, index) in messages"
              :key="index + (msg.isStreaming ? '_streaming' : '_done')"
              :class="['message', msg.role]"
            >
              <div class="message-avatar">
                <svg v-if="msg.role === 'user'" width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </div>
              <div class="message-bubble">
                <!-- 流式消息使用原始文本，已完成消息使用Markdown渲染 -->
                <div
                  class="message-content"
                  v-html="getMessageContent(msg)"
                ></div>
                <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
              </div>
            </div>

            <!-- 输入中状态 - 只在没有流式消息时显示 -->
            <div v-if="isTyping && !messages.some(m => m.isStreaming)" class="message assistant">
              <div class="message-avatar">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </div>
              <div class="message-bubble">
                <div class="message-content typing">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="chat-input-area">
            <div class="input-wrapper">
              <textarea
                v-model="inputMessage"
                @keydown.enter.exact.prevent="sendMessage"
                placeholder="输入消息..."
                rows="1"
                ref="inputRef"
              ></textarea>
            </div>
            <button
              class="send-btn"
              @click="sendMessage"
              :disabled="!inputMessage.trim() || isTyping"
              aria-label="发送消息"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import ChatButton from './ChatButton.vue'
import { sendMessage as sendChatMessage, sendMessageStreamFetch, createConversation, getConversations, getMessages, deleteConversation as deleteConversationApi } from '@/api/chat'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import * as marked from 'marked'

const isOpen = ref(false)
const inputMessage = ref('')
const messages = ref([
  { role: 'assistant', content: '你好！我是AI助手，有什么可以帮助你的吗？', timestamp: new Date() }
])

// 用于存储原始内容（流式更新时使用）
const rawContents = ref({})
const isTyping = ref(false)
const messagesContainer = ref(null)
const inputRef = ref(null)

// 会话相关
const conversationId = ref(null)
const userId = ref(null) // 用户ID，从localStorage获取或生成
const conversations = ref([])
const sidebarCollapsed = ref(false)

// 初始化用户ID
const initUserId = () => {
  // 从localStorage获取用户ID
  let storedUserId = localStorage.getItem('chatUserId')

  if (!storedUserId) {
    // 如果没有存储的用户ID，生成一个新的
    storedUserId = generateUserId()
    localStorage.setItem('chatUserId', storedUserId)
    console.log('【前端】生成新用户ID:', storedUserId)
  } else {
    console.log('【前端】使用现有用户ID:', storedUserId)
  }

  // 确保userId是数字类型
  userId.value = Number(storedUserId)
}

// 生成用户ID（使用时间戳+随机数，转换为数字）
const generateUserId = () => {
  const timestamp = Date.now()
  const random = Math.floor(Math.random() * 10000)
  // 使用时间戳和随机数生成一个数字ID
  return timestamp * 10000 + random
}

// 重置用户ID（开启新对话时调用）
const resetUserId = () => {
  const newUserId = generateUserId()
  localStorage.setItem('chatUserId', newUserId)
  userId.value = newUserId
  console.log('【前端】开启新对话，生成新用户ID:', newUserId)
}

// 拖拽相关
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const panelPosition = ref({ x: 0, y: 0 })

const panelStyle = computed(() => ({
  right: `${panelPosition.value.x}px`,
  bottom: `${panelPosition.value.y + 80}px`
}))

// 计算当前会话标题
const currentConversationTitle = computed(() => {
  if (!conversationId.value) return '新对话'
  const conv = conversations.value.find(c => String(c.id) === conversationId.value)
  return conv ? conv.title : '新对话'
})

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    nextTick(() => {
      inputRef.value?.focus()
      scrollToBottom()
      loadConversations()
    })
  }
}

const closeChat = () => {
  isOpen.value = false
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 加载会话列表
const loadConversations = async () => {
  try {
    const data = await getConversations(userId.value)
    conversations.value = data || []

    // 如果没有会话，创建一个新会话
    if (conversations.value.length === 0) {
      await createNewConversation()
    } else if (!conversationId.value) {
      // 如果有会话但没有选中的会话，选中第一个
      // 确保 conversationId 存储为字符串，避免精度丢失
      conversationId.value = String(conversations.value[0].id)
      await loadMessages(conversationId.value)
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 创建新会话
const createNewConversation = async () => {
  try {
    // 开启新对话时生成新的用户ID
    resetUserId()

    const data = await createConversation(userId.value, '新对话')
    // 确保 conversationId 存储为字符串，避免精度丢失
    conversationId.value = String(data.id)
    conversations.value.unshift(data)
    messages.value = [
      { role: 'assistant', content: '你好！我是AI助手，有什么可以帮助你的吗？', timestamp: new Date() }
    ]
    console.log('【前端】创建新会话成功，会话ID:', conversationId.value, '用户ID:', userId.value)
    nextTick(() => {
      inputRef.value?.focus()
    })
    return data
  } catch (error) {
    console.error('创建会话失败:', error)
    throw error
  }
}

// 选择会话
const selectConversation = async (conv) => {
  // 确保 conversationId 存储为字符串，避免精度丢失
  conversationId.value = String(conv.id)
  await loadMessages(conv.id)
}

// 加载会话消息
const loadMessages = async (convId) => {
  try {
    const data = await getMessages(convId)
    messages.value = data.map(msg => ({
      role: msg.role,
      content: msg.content,
      timestamp: msg.createdAt
    }))
    nextTick(() => {
      scrollToBottom()
    })
  } catch (error) {
    console.error('加载消息失败:', error)
  }
}

// 删除会话
const deleteConversation = async (convId) => {
  try {
    await deleteConversationApi(convId)
    conversations.value = conversations.value.filter(c => c.id !== convId)

    // 如果删除的是当前会话，切换到其他会话或创建新会话
    if (String(conversationId.value) === String(convId)) {
      if (conversations.value.length > 0) {
        // 确保 conversationId 存储为字符串，避免精度丢失
        conversationId.value = String(conversations.value[0].id)
        await loadMessages(conversationId.value)
      } else {
        await createNewConversation()
      }
    }
  } catch (error) {
    console.error('删除会话失败:', error)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatConversationTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now - d

  // 如果是今天
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 如果是昨天
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }

  // 如果是本周
  const weekStart = new Date(now)
  weekStart.setDate(weekStart.getDate() - weekStart.getDay())
  if (d >= weekStart) {
    return d.toLocaleDateString('zh-CN', { weekday: 'short' })
  }

  // 其他情况显示日期
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 配置 marked 选项
marked.setOptions({
  breaks: true, // 支持 GitHub 风格的换行
  gfm: true,    // 启用 GitHub 风格的 Markdown
  headerIds: false // 禁用自动生成的 header ID
})

// 获取消息内容（根据流式状态决定是否渲染Markdown）
const getMessageContent = (msg) => {
  if (!msg || !msg.content) return ''

  if (msg.isStreaming) {
    // 流式消息使用转义后的文本
    return escapeHtml(msg.content)
  } else {
    // 已完成消息使用Markdown渲染
    return formatMessage(msg.content)
  }
}

// 格式化消息内容，支持 Markdown 和代码高亮
const formatMessage = (content) => {
  if (!content) return ''

  try {
    console.log('【格式化消息】开始渲染，内容长度:', content.length)
    console.log('【格式化消息】内容前200字符:', content.substring(0, 200))

    // 使用 marked 渲染 Markdown（不使用 highlight 选项）
    let html = marked.parse(content)

    console.log('【格式化消息】marked.parse 完成，HTML长度:', html.length)
    console.log('【格式化消息】原始HTML:', html.substring(0, 200))

    // 手动处理代码块高亮
    html = html.replace(/<pre><code class="language-(\w+)">([\s\S]*?)<\/code><\/pre>/g, (match, lang, code) => {
      console.log('【格式化消息】处理代码块，语言:', lang)
      try {
        // 解码 HTML 实体
        const decodedCode = code.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&amp;/g, '&')
        let highlightedCode
        if (lang && hljs.getLanguage(lang)) {
          highlightedCode = hljs.highlight(decodedCode, { language: lang }).value
        } else {
          highlightedCode = hljs.highlightAuto(decodedCode).value
        }
        console.log('【格式化消息】高亮成功，HTML长度:', highlightedCode.length)
        return `<pre><code class="hljs language-${lang}">${highlightedCode}</code></pre>`
      } catch (err) {
        console.log('【格式化消息】高亮失败:', err)
        return match
      }
    })

    // 处理行内 code（不在 pre 标签内的）
    // 先保存 pre 标签，避免影响
    const preTags = []
    html = html.replace(/<pre[^>]*>[\s\S]*?<\/pre>/g, (match) => {
      preTags.push(match)
      return `__PRE_TAG_${preTags.length - 1}__`
    })

    // 处理行内 code
    html = html.replace(/<code>([^<]+)<\/code>/g, '<code class="inline-code">$1</code>')

    // 恢复 pre 标签
    preTags.forEach((tag, index) => {
      html = html.replace(`__PRE_TAG_${index}__`, tag)
    })

    console.log('【格式化消息】处理后HTML:', html.substring(0, 200))
    console.log('【格式化消息】是否包含pre标签:', html.includes('<pre'))

    return html
  } catch (error) {
    console.error('【格式化消息】Markdown 渲染失败:', error)
    // 如果渲染失败，返回转义后的文本
    return escapeHtml(content)
  }
}

// 重新应用代码高亮（仅在需要时调用）
const applyCodeHighlight = () => {
  // 找到所有代码块并重新应用高亮
  const codeBlocks = document.querySelectorAll('.message-content pre code')
  codeBlocks.forEach(block => {
    // 如果已经高亮过（包含hljs类），跳过
    if (block.classList.contains('hljs') || block.querySelector('.hljs')) return

    // 获取语言类名
    const classes = block.className.split(' ')
    const languageClass = classes.find(c => c.startsWith('language-'))
    const language = languageClass ? languageClass.replace('language-', '') : 'plaintext'

    const code = block.textContent
    if (code) {
      try {
        const highlightedCode = hljs.highlight(code, { language }).value
        block.innerHTML = highlightedCode
      } catch (e) {
        // 如果高亮失败，使用自动检测
        const highlightedCode = hljs.highlightAuto(code).value
        block.innerHTML = highlightedCode
      }
    }
  })
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isTyping.value) return

  // 如果没有会话ID，创建新会话
  if (!conversationId.value) {
    console.log('【前端】没有会话ID，创建新会话')
    await createNewConversation()
  }

  // 添加用户消息
  messages.value.push({ role: 'user', content, timestamp: new Date() })
  inputMessage.value = ''
  scrollToBottom()

  // 创建临时AI消息用于流式更新
  const assistantMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    timestamp: new Date(),
    isStreaming: true  // 标记为流式消息
  })

  // 设置输入中状态（流式消息开始后，不再显示输入中状态）
  isTyping.value = false
  scrollToBottom()

  try {
    // 使用流式调用
    await sendMessageStreamFetch(
      {
        userId: userId.value,
        message: content,
        conversationId: conversationId.value ? String(conversationId.value) : null
      },
      // onChunk: 数据块回调
      (chunk) => {
        // 更新AI消息内容
        messages.value[assistantMessageIndex].content += chunk
        scrollToBottom()
      },
      // onComplete: 完成回调
      (result) => {
        // 更新会话ID（始终更新，确保会话连续性）
        if (result && result.conversationId) {
          // 确保 conversationId 存储为字符串，避免精度丢失
          conversationId.value = String(result.conversationId)
          console.log('【前端】更新会话ID:', conversationId.value)

          // 更新会话列表中的会话标题
          const convIndex = conversations.value.findIndex(c => String(c.id) === conversationId.value)
          if (convIndex !== -1) {
            // 如果会话已存在，更新其标题
            const conv = conversations.value[convIndex]
            if (conv.title === '新对话' && content.length > 0) {
              conv.title = content.length > 20 ? content.substring(0, 20) + '...' : content
            }
          } else {
            // 如果会话不存在于列表中，重新加载会话列表
            loadConversations()
          }
        }

        // 标记消息为已完成（停止流式渲染）
        if (assistantMessageIndex < messages.value.length) {
          // 强制更新消息对象，触发Vue重新渲染
          messages.value[assistantMessageIndex] = {
            ...messages.value[assistantMessageIndex],
            isStreaming: false
          }
        }

        isTyping.value = false
        // 等待DOM更新后再滚动
        nextTick(() => {
          scrollToBottom()
        })
      },
      // onError: 错误回调
      (error) => {
        console.error('流式消息失败:', error)
        messages.value[assistantMessageIndex].content = '抱歉，发送消息失败，请稍后重试。'
        messages.value[assistantMessageIndex].isStreaming = false
        isTyping.value = false
        scrollToBottom()
      }
    )
  } catch (error) {
    console.error('发送消息失败:', error)
    if (assistantMessageIndex < messages.value.length) {
      messages.value[assistantMessageIndex].content = '抱歉，发送消息失败，请稍后重试。'
      messages.value[assistantMessageIndex].isStreaming = false
    }
    isTyping.value = false
    scrollToBottom()
  }
}

// 拖拽功能 - 只允许点击标题栏拖拽
const startDrag = (e) => {
  // 只允许点击标题栏（.chat-header）拖拽，点击其他区域不拖拽
  const header = e.target.closest('.chat-header')
  if (!header) return

  // 阻止点击标题栏内的按钮时拖拽
  if (e.target.closest('.header-btn')) return

  isDragging.value = true
  dragOffset.value = {
    x: e.clientX,
    y: e.clientY
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!isDragging.value) return
  const deltaX = dragOffset.value.x - e.clientX
  const deltaY = dragOffset.value.y - e.clientY
  panelPosition.value.x = Math.max(0, panelPosition.value.x + deltaX)
  panelPosition.value.y = Math.max(0, panelPosition.value.y + deltaY)
  dragOffset.value = { x: e.clientX, y: e.clientY }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 转义HTML字符（用于流式消息）
const escapeHtml = (text) => {
  if (!text) return ''
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// 测试Markdown渲染
const testMarkdownRendering = () => {
  const testContent = `
这是一个测试消息，包含代码块：

\`\`\`java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
\`\`\`

还有行内代码：\`console.log("test")\`

以及列表：
- 项目1
- 项目2
- 项目3
`

  console.log('【测试】测试Markdown渲染')
  const html = formatMessage(testContent)
  console.log('【测试】渲染结果:', html)
}

onMounted(() => {
  // 初始化位置
  panelPosition.value = { x: 20, y: 20 }

  // 初始化用户ID
  initUserId()

  // 加载会话列表
  loadConversations()

  // 测试Markdown渲染
  testMarkdownRendering()

  // 测试marked是否工作
  console.log('【测试】marked库:', typeof marked)
  console.log('【测试】marked.parse:', typeof marked.parse)

  // 简单测试
  const testMarkdown = '# 测试标题\n\n```java\npublic class Test {\n    public static void main(String[] args) {\n        System.out.println("Hello");\n    }\n}\n```'
  try {
    const result = marked.parse(testMarkdown)
    console.log('【测试】marked解析结果:', result.substring(0, 200))
  } catch (e) {
    console.error('【测试】marked解析失败:', e)
  }
})
</script>

<style scoped>
.chat-window-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  max-width: 100vw;
  max-height: 100vh;
}

.chat-panel {
  position: fixed;
  width: 640px;
  height: 600px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 20px;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(0, 0, 0, 0.05),
    0 10px 40px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;
  box-sizing: border-box;
  max-width: 100vw;
  max-height: 100vh;
}

/* 侧边栏样式 */
.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #f8f9fa 0%, #e9ecef 100%);
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.sidebar-collapsed {
  width: 56px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.5);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #6c757d;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.toggle-sidebar-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c757d;
  transition: all 0.2s ease;
}

.toggle-sidebar-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #495057;
  transform: scale(1.05);
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.new-conversation-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  color: #495057;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.03);
}

.new-conversation-btn:hover {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-color: rgba(0, 123, 255, 0.3);
  color: #007bff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 123, 255, 0.1);
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: transparent;
}

.conversation-item:hover {
  background: rgba(0, 0, 0, 0.04);
  transform: translateX(2px);
}

.conversation-item.active {
  background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.25);
}

.conversation-item.active .conversation-time {
  color: rgba(255, 255, 255, 0.7);
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.conversation-time {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
  line-height: 1.3;
}

.delete-conversation-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #adb5bd;
  opacity: 0;
  transition: all 0.2s ease;
}

.conversation-item:hover .delete-conversation-btn {
  opacity: 1;
}

.delete-conversation-btn:hover {
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
  transform: scale(1.1);
}

/* 主聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  width: 100%;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  color: #333;
  cursor: move;
  user-select: none;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
}

.chat-title {
  font-size: 15px;
  font-weight: 600;
  color: #212529;
  letter-spacing: -0.2px;
}

.header-right {
  display: flex;
  gap: 8px;
}

.header-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c757d;
  transition: all 0.2s ease;
}

.header-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #495057;
  transform: scale(1.05);
}

.header-btn:active {
  transform: scale(0.95);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  min-height: 0;
  box-sizing: border-box;
  width: 100%;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 85%;
  animation: message-in 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
}

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.25);
  transition: transform 0.2s ease;
}

.message.user .message-avatar {
  background: linear-gradient(135deg, #6c757d 0%, #495057 100%);
  box-shadow: 0 4px 12px rgba(108, 117, 125, 0.25);
}

.message:hover .message-avatar {
  transform: scale(1.05);
}

.message-bubble {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.message-content {
  background: #ffffff;
  padding: 14px 18px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.7;
  color: #212529;
  word-wrap: break-word;
  word-break: break-word;
  max-width: 100%;
  overflow-wrap: break-word;
  box-sizing: border-box;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.message.user .message-content {
  background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.25);
  border: none;
}

.message.assistant .message-content {
  background: #ffffff;
  text-align: left;
}

/* 段落样式 */
.message-content p {
  margin: 0 0 14px 0;
  line-height: 1.8;
  color: #212529;
}

.message-content p:last-child {
  margin-bottom: 0;
}

.message.user .message-content p {
  color: rgba(255, 255, 255, 0.95);
}

/* Markdown 标题样式 */
.message-content h1,
.message-content h2,
.message-content h3,
.message-content h4,
.message-content h5,
.message-content h6 {
  margin: 18px 0 14px 0;
  font-weight: 700;
  line-height: 1.3;
  text-indent: 0;
  color: #212529;
}

.message.user .message-content h1,
.message.user .message-content h2,
.message.user .message-content h3,
.message.user .message-content h4,
.message.user .message-content h5,
.message.user .message-content h6 {
  color: rgba(255, 255, 255, 0.95);
}

.message-content h1 {
  font-size: 1.6em;
  border-bottom: 2px solid rgba(0, 0, 0, 0.08);
  padding-bottom: 0.4em;
}

.message-content h2 {
  font-size: 1.4em;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  padding-bottom: 0.3em;
}

.message-content h3 {
  font-size: 1.25em;
}

.message-content h4 {
  font-size: 1.15em;
}

.message-content h5,
.message-content h6 {
  font-size: 1.05em;
}

/* Markdown 列表样式 */
.message-content ul,
.message-content ol {
  margin: 14px 0;
  padding-left: 1.8em;
  text-indent: 0;
}

.message-content li {
  margin: 8px 0;
  line-height: 1.7;
  color: #212529;
}

.message.user .message-content li {
  color: rgba(255, 255, 255, 0.9);
}

.message-content ul {
  list-style-type: disc;
}

.message-content ol {
  list-style-type: decimal;
}

/* Markdown 引用样式 */
.message-content blockquote {
  margin: 14px 0;
  padding: 12px 16px;
  border-left: 4px solid #007bff;
  background: linear-gradient(90deg, rgba(0, 123, 255, 0.05) 0%, transparent 100%);
  color: #495057;
  text-indent: 0;
  font-style: italic;
  border-radius: 0 12px 12px 0;
}

.message.user .message-content blockquote {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
  border-left-color: rgba(255, 255, 255, 0.5);
}

/* Markdown 链接样式 */
.message-content a {
  color: #007bff;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s ease;
}

.message-content a:hover {
  color: #0056b3;
  text-decoration: underline;
}

.message.user .message-content a {
  color: rgba(255, 255, 255, 0.9);
}

.message.user .message-content a:hover {
  color: #ffffff;
}

/* Markdown 分割线 */
.message-content hr {
  margin: 18px 0;
  border: none;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

/* 代码块样式 - 暗色主题 */
.message-content pre,
.message-content pre code {
  background: #282c34 !important;
}

.message-content pre {
  border-radius: 8px;
  padding: 16px !important;
  margin: 12px 0 !important;
  overflow-x: auto !important;
  overflow-y: hidden !important;
  font-size: 13px !important;
  line-height: 1.5 !important;
  text-indent: 0 !important;
  border: 1px solid #181a1f !important;
  box-sizing: border-box !important;
  max-width: 100% !important;
  min-width: 0 !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
  width: 100% !important;
  display: block !important;
  word-wrap: normal !important;
}

.message-content pre code {
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace !important;
  background: #282c34 !important;
  padding: 0 !important;
  color: #abb2bf !important;
  display: block !important;
  white-space: pre !important;
  word-wrap: normal !important;
  overflow-x: auto !important;
  min-width: 100% !important;
  box-sizing: border-box !important;
  max-width: 100% !important;
  width: 100% !important;
  text-align: left !important;
}

/* 代码块滚动条样式 */
.message-content pre::-webkit-scrollbar {
  height: 8px;
}

.message-content pre::-webkit-scrollbar-track {
  background: #21252b;
  border-radius: 4px;
}

.message-content pre::-webkit-scrollbar-thumb {
  background: #3e4451;
  border-radius: 4px;
}

.message-content pre::-webkit-scrollbar-thumb:hover {
  background: #4b5263;
}

/* 行内代码样式 */
.message-content code:not(pre code) {
  background: #3e4451;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
  color: #abb2bf;
  border: 1px solid #4b5263;
}

/* 确保代码块内的换行正确显示 */
.message-content pre code .hljs {
  white-space: pre;
  word-wrap: normal;
}

/* 确保代码块在消息内容中正确显示 */
.message-content pre {
  max-width: 100%;
  box-sizing: border-box;
}

/* 优化代码块在消息中的显示 */
.message-content {
  max-width: 100%;
  box-sizing: border-box;
}

/* 行内代码样式 */
.message-content .inline-code {
  background: #3e4451;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
  color: #abb2bf;
  border: 1px solid #4b5263;
}

/* 暗色主题代码高亮样式 */
.hljs,
.hljs code,
.hljs .hljs {
  background: #282c34 !important;
  padding: 0 !important;
  color: #abb2bf !important;
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace !important;
}

.hljs-keyword,
.hljs-selector-tag,
.hljs-built_in,
.hljs-name,
.hljs-tag {
  color: #c678dd !important;
}

.hljs-string,
.hljs-title,
.hljs-section,
.hljs-attribute,
.hljs-literal,
.hljs-template-tag,
.hljs-template-variable,
.hljs-type,
.hljs-addition {
  color: #98c379 !important;
}

.hljs-comment,
.hljs-quote,
.hljs-deletion,
.hljs-meta {
  color: #5c6370 !important;
}

.hljs-number,
.hljs-regexp,
.hljs-variable,
.hljs-template-variable {
  color: #d19a66 !important;
}

.hljs-keyword,
.hljs-selector-tag,
.hljs-literal,
.hljs-title,
.hljs-section,
.hljs-doctag,
.hljs-type,
.hljs-name,
.hljs-strong {
  font-weight: normal !important;
}

.hljs-function {
  color: #61afef !important;
}

.hljs-params {
  color: #d19a66 !important;
}

/* 表格样式 */
.message-content table {
  border-collapse: collapse;
  margin: 12px 0;
  width: 100%;
  text-indent: 0;
}

.message-content th,
.message-content td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.message-content th {
  background: #f8f9fa;
  font-weight: 600;
}

.message-content tr:nth-child(even) {
  background: #f8f9fa;
}

/* 消息内容滚动条样式 */
.message-content::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

.message-content::-webkit-scrollbar-track {
  background: transparent;
}

.message-content::-webkit-scrollbar-thumb {
  background: #999;
  border-radius: 2px;
}

.message-content::-webkit-scrollbar-thumb:hover {
  background: #666;
}

.message-time {
  font-size: 11px;
  color: #adb5bd;
  padding: 0 4px;
  font-weight: 400;
}

.message.user .message-time {
  text-align: right;
  color: rgba(255, 255, 255, 0.6);
}

.typing {
  display: flex;
  gap: 6px;
  padding: 6px 0;
  align-items: center;
}

.typing span {
  width: 10px;
  height: 10px;
  background: #adb5bd;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing span:nth-child(1) { animation-delay: -0.32s; }
.typing span:nth-child(2) { animation-delay: -0.16s; }
.typing span:nth-child(3) { animation-delay: 0s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input-area {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
  flex-shrink: 0;
}

.input-wrapper {
  flex: 1;
  position: relative;
}

.chat-input-area textarea {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 24px;
  padding: 14px 20px;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: all 0.2s ease;
  font-family: inherit;
  background: #ffffff;
  min-height: 48px;
  max-height: 120px;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.chat-input-area textarea:focus {
  border-color: rgba(0, 123, 255, 0.4);
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(0, 123, 255, 0.1);
}

.chat-input-area textarea::placeholder {
  color: #adb5bd;
}

.send-btn {
  width: 44px;
  height: 44px;
  border: none;
  background: linear-gradient(135deg, #007bff 0%, #0056b3 100%);
  border-radius: 14px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #0056b3 0%, #004085 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 123, 255, 0.4);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.3);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(30px) scale(0.92);
}

/* 聊天消息容器滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #dee2e6;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #ced4da;
}
</style>
