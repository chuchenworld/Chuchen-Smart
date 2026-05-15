<template>
  <button class="chat-button" @click="$emit('click')" aria-label="打开AI对话">
    <div class="button-icon">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
    </div>
    <div class="button-pulse"></div>
    <div class="button-glow"></div>
  </button>
</template>

<script setup>
defineEmits(['click'])
</script>

<style scoped>
.chat-button {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 52px;
  height: 52px;
  border: none;
  border-radius: 50%;
  background: #007bff;
  color: white;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
  transition: all 0.2s ease;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-button:hover {
  background: #0056b3;
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 123, 255, 0.4);
}

.chat-button:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.3);
}

.chat-button:active {
  transform: scale(0.95);
}

.button-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
