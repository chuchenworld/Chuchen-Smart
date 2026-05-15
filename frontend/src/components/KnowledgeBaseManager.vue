<template>
  <div class="knowledge-base-manager">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <h3>知识库管理</h3>
      <div class="toolbar-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索知识库..."
          clearable
          @keyup.enter="searchKnowledge"
          style="width: 200px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加知识
        </el-button>
      </div>
    </div>

    <!-- 知识库列表 -->
    <div class="knowledge-list">
      <el-table :data="knowledgeList" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="knowledge-title">{{ row.title }}</div>
            <div class="knowledge-category">
              <el-tag size="small">{{ row.category || '未分类' }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300">
          <template #default="{ row }">
            <div class="knowledge-content">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="editKnowledge(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button type="text" size="small" @click="deleteKnowledge(row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑知识' : '添加知识'"
      width="500px"
    >
      <el-form :model="knowledgeForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="knowledgeForm.title" placeholder="请输入标题"></el-input>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="knowledgeForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入知识内容"
          ></el-input>
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="knowledgeForm.category" placeholder="请输入分类"></el-input>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="knowledgeForm.tags" placeholder="请输入标签（逗号分隔）"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKnowledge" :loading="saving">
          {{ isEditing ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'

const userId = ref(1) // 实际项目中应从登录状态获取
const knowledgeList = ref([])
const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEditing = ref(false)
const knowledgeForm = ref({
  id: null,
  title: '',
  content: '',
  category: '',
  tags: ''
})

// 加载知识库列表
const loadKnowledgeList = async () => {
  loading.value = true
  try {
    const response = await fetch(`/api/knowledge?userId=${userId.value}`)
    const data = await response.json()
    if (data.code === 200) {
      knowledgeList.value = data.data || []
    } else {
      ElMessage.error(data.message || '加载知识库失败')
    }
  } catch (error) {
    console.error('加载知识库失败:', error)
    ElMessage.error('加载知识库失败')
  } finally {
    loading.value = false
  }
}

// 搜索知识库
const searchKnowledge = async () => {
  if (!searchKeyword.value.trim()) {
    loadKnowledgeList()
    return
  }

  loading.value = true
  try {
    const response = await fetch(`/api/knowledge/search?userId=${userId.value}&keyword=${encodeURIComponent(searchKeyword.value)}`)
    const data = await response.json()
    if (data.code === 200) {
      knowledgeList.value = data.data || []
    } else {
      ElMessage.error(data.message || '搜索失败')
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 显示添加对话框
const showAddDialog = () => {
  isEditing.value = false
  knowledgeForm.value = {
    id: null,
    title: '',
    content: '',
    category: '',
    tags: ''
  }
  dialogVisible.value = true
}

// 编辑知识
const editKnowledge = (knowledge) => {
  isEditing.value = true
  knowledgeForm.value = { ...knowledge }
  dialogVisible.value = true
}

// 保存知识
const saveKnowledge = async () => {
  if (!knowledgeForm.value.title || !knowledgeForm.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  saving.value = true
  try {
    const url = isEditing.value
      ? `/api/knowledge/${knowledgeForm.value.id}`
      : '/api/knowledge'

    const method = isEditing.value ? 'PUT' : 'POST'

    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...knowledgeForm.value,
        userId: userId.value
      })
    })

    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success(isEditing.value ? '保存成功' : '添加成功')
      dialogVisible.value = false
      loadKnowledgeList()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    console.error('保存知识失败:', error)
    ElMessage.error('保存知识失败')
  } finally {
    saving.value = false
  }
}

// 删除知识
const deleteKnowledge = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条知识吗？', '提示', {
      type: 'warning'
    })

    const response = await fetch(`/api/knowledge/${id}`, {
      method: 'DELETE'
    })

    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success('删除成功')
      loadKnowledgeList()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除知识失败:', error)
      ElMessage.error('删除知识失败')
    }
  }
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadKnowledgeList()
})
</script>

<style scoped>
.knowledge-base-manager {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.toolbar h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.knowledge-list {
  margin-top: 10px;
}

.knowledge-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.knowledge-category {
  margin-top: 4px;
}

.knowledge-content {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
