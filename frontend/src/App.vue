<script setup>
import ChatWindow from './components/ChatWindow/ChatWindow.vue'
</script>

<template>
  <div class="app-container">
    <!-- 简单的背景 -->
    <div class="background">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
    </div>

    <!-- 悬浮窗 -->
    <ChatWindow />
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  background: #f5f5f5;
  position: relative;
  overflow: hidden;
}

.background {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.3;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #e0e0e0 0%, #f0f0f0 100%);
  top: -100px;
  right: -100px;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #d0d0d0 0%, #e8e8e8 100%);
  bottom: -50px;
  left: -50px;
}
</style>
