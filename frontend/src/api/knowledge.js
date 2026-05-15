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
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
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
 * 添加知识库条目
 * @param {Object} knowledge - 知识库对象
 * @returns {Promise} 响应数据
 */
export const addKnowledge = (knowledge) => {
  return api.post('/knowledge', knowledge)
}

/**
 * 搜索知识库（关键词搜索）
 * @param {number} userId - 用户ID
 * @param {string} keyword - 关键词
 * @returns {Promise} 响应数据
 */
export const searchKnowledge = (userId, keyword) => {
  return api.get('/knowledge/search', {
    params: { userId, keyword }
  })
}

/**
 * 获取用户知识库列表
 * @param {number} userId - 用户ID
 * @returns {Promise} 响应数据
 */
export const getUserKnowledge = (userId) => {
  return api.get('/knowledge', {
    params: { userId }
  })
}

/**
 * 删除知识库条目
 * @param {number|string} knowledgeId - 知识库ID
 * @returns {Promise} 响应数据
 */
export const deleteKnowledge = (knowledgeId) => {
  return api.delete(`/knowledge/${String(knowledgeId)}`)
}

/**
 * 更新知识库条目
 * @param {number|string} knowledgeId - 知识库ID
 * @param {Object} knowledge - 知识库对象
 * @returns {Promise} 响应数据
 */
export const updateKnowledge = (knowledgeId, knowledge) => {
  return api.put(`/knowledge/${String(knowledgeId)}`, knowledge)
}

export default {
  addKnowledge,
  searchKnowledge,
  getUserKnowledge,
  deleteKnowledge,
  updateKnowledge
}
