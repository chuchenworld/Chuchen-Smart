import axios from 'axios'

// 创建 axios 实例
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加认证 token
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`
    // }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    // 如果后端返回的结构是 { code, message, data }
    if (response.data && response.data.code === 200) {
      return response.data.data
    }
    return response.data
  },
  error => {
    console.error('API 请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 发送消息并获取AI回复（非流式）
 * @param {Object} request - 请求参数
 * @param {number} request.userId - 用户ID
 * @param {string} request.message - 消息内容
 * @param {number} [request.conversationId] - 会话ID（可选）
 * @param {string} [request.model] - AI模型（可选）
 * @param {number} [request.temperature] - 温度参数（可选）
 * @returns {Promise} 响应数据
 */
export const sendMessage = (request) => {
  return api.post('/chat/send', request)
}

/**
 * 发送消息并获取AI回复（流式）
 * @param {Object} request - 请求参数
 * @param {number} request.userId - 用户ID
 * @param {string} request.message - 消息内容
 * @param {number} [request.conversationId] - 会话ID（可选）
 * @param {Function} onChunk - 数据块回调函数
 * @param {Function} onComplete - 完成回调函数
 * @param {Function} onError - 错误回调函数
 */
export const sendMessageStream = (request, onChunk, onComplete, onError) => {
  return new Promise((resolve, reject) => {
    // 创建 EventSource 连接
    const eventSource = new EventSource(`/api/chat/stream?request=${encodeURIComponent(JSON.stringify(request))}`)

    // 监听数据块事件
    eventSource.addEventListener('chunk', (event) => {
      if (onChunk) {
        onChunk(event.data)
      }
    })

    // 监听完成事件
    eventSource.addEventListener('complete', (event) => {
      if (onComplete) {
        try {
          const data = JSON.parse(event.data)
          onComplete(data)
        } catch (e) {
          onComplete(event.data)
        }
      }
      eventSource.close()
      resolve()
    })

    // 监听错误事件
    eventSource.addEventListener('error', (event) => {
      if (onError) {
        onError(event.data)
      }
      eventSource.close()
      reject(new Error(event.data))
    })

    // 监听连接错误
    eventSource.onerror = (error) => {
      console.error('EventSource 错误:', error)
      if (onError) {
        onError('连接错误')
      }
      eventSource.close()
      reject(new Error('连接错误'))
    }
  })
}

/**
 * 发送消息并获取AI回复（流式）- 使用 fetch API
 * @param {Object} request - 请求参数
 * @param {number} request.userId - 用户ID
 * @param {string} request.message - 消息内容
 * @param {number} [request.conversationId] - 会话ID（可选）
 * @param {Function} onChunk - 数据块回调函数
 * @param {Function} onComplete - 完成回调函数
 * @param {Function} onError - 错误回调函数
 */
export const sendMessageStreamFetch = async (request, onChunk, onComplete, onError) => {
  try {
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 处理 SSE 格式的数据
      const lines = buffer.split('\n')
      buffer = lines.pop() // 保留未完成的行

      for (const line of lines) {
        // 处理 "data: " 和 "data:" 两种格式
        let data = null
        if (line.startsWith('data: ')) {
          data = line.substring(6)
        } else if (line.startsWith('data:')) {
          data = line.substring(5)
        }

        if (data) {
          console.log('【API】收到数据:', data.substring(0, 100))
          try {
            const parsed = JSON.parse(data)
            console.log('【API】解析后的JSON:', parsed)
            if (parsed.done) {
              // 完成标记，包含 conversationId
              if (onComplete) onComplete(parsed)
              return
            }
            if (parsed.content) {
              console.log('【API】提取content:', parsed.content.substring(0, 50))
              if (onChunk) onChunk(parsed.content)
            }
          } catch (e) {
            console.log('解析错误:', e.message, '数据:', data)
          }
        }
      }
    }

    if (onComplete) onComplete()
  } catch (error) {
    console.error('流式请求错误:', error)
    if (onError) onError(error.message)
  }
}

/**
 * 创建新会话
 * @param {number} userId - 用户ID
 * @param {string} [title] - 会话标题（可选）
 * @returns {Promise} 会话信息
 */
export const createConversation = (userId, title) => {
  const params = new URLSearchParams()
  params.append('userId', userId)
  if (title) {
    params.append('title', title)
  }
  return api.post('/chat/conversation', null, { params })
}

/**
 * 获取用户会话列表
 * @param {number} userId - 用户ID
 * @returns {Promise} 会话列表
 */
export const getConversations = (userId) => {
  return api.get('/chat/conversations', { params: { userId } })
}

/**
 * 获取会话消息列表
 * @param {number|string} conversationId - 会话ID
 * @returns {Promise} 消息列表
 */
export const getMessages = (conversationId) => {
  return api.get('/chat/messages', { params: { conversationId: String(conversationId) } })
}

/**
 * 删除会话
 * @param {number|string} conversationId - 会话ID
 * @returns {Promise} 删除结果
 */
export const deleteConversation = (conversationId) => {
  return api.delete(`/chat/conversation/${String(conversationId)}`)
}

export default {
  sendMessage,
  sendMessageStream,
  sendMessageStreamFetch,
  createConversation,
  getConversations,
  getMessages,
  deleteConversation
}
