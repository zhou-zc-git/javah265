<template>
  <div id="app">
    <h1>Vue RTSP HLS 播放器</h1>
    <div v.if="!streamUrl"> <!-- 如果没有流URL，则显示输入区域 -->
        <input v-model="rtspInputUrl" placeholder="输入 RTSP URL (例如 rtsp://...)" />
        <button @click="fetchStreamUrl" :disabled="loadingStream">
            {{ loadingStream ? '加载中...' : '开始推流' }}
        </button>
        <p v-if="backendError" style="color:red;">后端错误: {{ backendError }}</p>
    </div>

    <div v-if="streamUrl"> <!-- 如果有流URL，则显示播放器 -->
        <HlsPlayer :hlsSrc="streamUrl" :autoplay="true" />
        <button @click="stopTheStream">停止推流</button>
    </div>
    <p>流媒体 URL: {{ streamUrl }}</p>
    <p>流媒体 ID: {{ streamId }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import HlsPlayer from './components/HlsPlayer.vue'; // 导入 HLS 播放器组件，请根据实际路径调整

const rtspInputUrl = ref('rtsp://your_rtsp_stream_url_here'); // 默认或示例 RTSP URL
const streamUrl = ref(''); // 这将是从后端获取的 .m3u8 URL
const streamId = ref(''); // 用于存储流ID，以便停止流
const loadingStream = ref(false); // 是否正在加载流
const backendError = ref(''); // 后端返回的错误信息
const backendBaseUrl = 'http://localhost:9198'; // Java 后端的基础 URL (更新为9198端口)

// 从后端获取 HLS 流 URL
const fetchStreamUrl = async () => {
  if (!rtspInputUrl.value) {
    alert('请输入 RTSP URL。');
    return;
  }
  loadingStream.value = true;
  backendError.value = '';
  streamUrl.value = ''; // 清除之前的流
  streamId.value = '';  // 清除之前的流ID

  try {
    // 调用后端 API 启动流
    const response = await fetch(`${backendBaseUrl}/start_stream?rtsp_url=${encodeURIComponent(rtspInputUrl.value)}`);
    if (!response.ok) { // 如果响应状态码不是 2xx
      const errorText = await response.text(); // 获取错误文本
      throw new Error(`启动流失败: ${response.status} ${errorText || ''}`);
    }
    const playlistUrl = await response.text(); // 获取播放列表 URL
    streamUrl.value = playlistUrl;

    // 从播放列表 URL 中提取 streamId
    // (假设格式为 http://.../hls/<streamId>/stream.m3u8)
    // 这是一个简单的提取方法，可能需要根据实际情况调整
    const parts = playlistUrl.split('/');
    if (parts.length >= 3) {
        streamId.value = parts[parts.length - 2];
    }
    console.log('流已启动。HLS URL:', streamUrl.value, "流 ID:", streamId.value);

  } catch (error) {
    console.error('获取流 URL 时出错:', error);
    backendError.value = error.message; // 显示错误信息
    streamUrl.value = ''; // 出错时清空流 URL
  } finally {
    loadingStream.value = false; // 结束加载状态
  }
};

// 停止当前流
const stopTheStream = async () => {
    if (!streamId.value) {
        alert("没有活动的流ID可以停止。");
        return;
    }
    try {
        // 调用后端 API 停止流
        const response = await fetch(`${backendBaseUrl}/stop_stream?streamId=${streamId.value}`);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`停止流失败: ${response.status} ${errorText || ''}`);
        }
        alert(await response.text()); // 显示后端返回的成功信息
        streamUrl.value = ''; // 清空流 URL
        streamId.value = '';  // 清空流 ID
    } catch (error) {
        console.error('停止流时出错:', error);
        backendError.value = error.message; // 显示错误信息
    }
};

</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif; /* 字体 */
  text-align: center; /* 文本居中 */
  color: #2c3e50; /* 文本颜色 */
  margin-top: 20px; /* 顶部外边距 */
}
input {
  padding: 8px; /* 内边距 */
  margin-right: 8px; /* 右外边距 */
  width: 300px; /* 宽度 */
}
button {
  padding: 8px 12px; /* 内边距 */
  cursor: pointer; /* 鼠标指针样式 */
}
</style>
