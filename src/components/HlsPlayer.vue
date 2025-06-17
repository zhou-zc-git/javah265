<template>
  <div class.vue="video-container">
    <video ref="videoPlayer" controls autoplay class="video-player"></video>
    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, defineProps } from 'vue';
import Hls from 'hls.js'; // 导入 hls.js 库

// 定义组件的 props
const props = defineProps({
  hlsSrc: { // HLS 流的 URL (.m3u8)
    type: String,
    required: true,
  },
  autoplay: { // 是否自动播放
    type: Boolean,
    default: true,
  }
});

const videoPlayer = ref(null); // video 元素的模板引用
let hls = null; // hls.js 实例
const errorMessage = ref(''); // 用于在模板中显示错误信息

// 初始化播放器函数
const initPlayer = () => {
  if (Hls.isSupported()) { // 检查当前浏览器是否支持 HLS.js
    if (hls) {
      hls.destroy(); // 如果已存在 hls 实例，先销毁
    }
    hls = new Hls({
      // 可选的 hls.js 配置项
      // debug: true, // 开发时可以启用调试模式
      // autoStartLoad: props.autoplay, // 控制是否在 hls.js 附加到 media 元素后自动开始加载
      maxBufferLength: 30, // 最大缓冲长度 (秒)
      maxMaxBufferLength: 600, // 最大允许的缓冲长度 (秒)
      // manifestLoadingTimeOut: 10000, // manifest 加载超时时间 (毫秒)
      // manifestLoadingMaxRetry: 3, // manifest 加载最大重试次数
    });

    if (videoPlayer.value) { // 确保 video 元素已存在
      hls.attachMedia(videoPlayer.value); // 将 hls.js 实例附加到 video 元素
      hls.loadSource(props.hlsSrc); // 加载 HLS 源

      hls.on(Hls.Events.MANIFEST_PARSED, () => { // 当 manifest 解析完成时
        if (props.autoplay) {
          videoPlayer.value.play().catch(error => {
            console.warn('自动播放被浏览器阻止:', error);
            // 自动播放可能会被浏览器策略阻止
            // 你可能需要向用户显示一个播放按钮或提示信息
            errorMessage.value = '自动播放被阻止。请点击播放按钮开始。';
          });
        }
        errorMessage.value = ''; // 清除之前的错误信息
      });

      hls.on(Hls.Events.ERROR, (event, data) => { // 监听 HLS.js 错误事件
        if (data.fatal) { // 如果是致命错误
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              console.error('遇到致命网络错误，尝试恢复:', data);
              // hls.startLoad(); // 可以选择尝试恢复加载
              errorMessage.value = `网络错误: ${data.details}。可能需要重试。`;
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              console.error('遇到致命媒体错误:', data);
              // hls.recoverMediaError(); // 可以选择尝试恢复媒体错误
              errorMessage.value = `媒体错误: ${data.details}。流可能已损坏。`;
              break;
            default:
              console.error('发生不可恢复的错误:', data);
              errorMessage.value = `播放器错误: ${data.details}。`;
              hls.destroy(); // 发生其他致命错误时销毁 hls 实例
              break;
          }
        } else {
            console.warn('非致命 HLS 错误:', data.details);
            // errorMessage.value = `警告: ${data.details}`; // 也可以选择显示非致命错误
        }
      });
    } else {
        console.error("未找到用于 HLS.js 附加的 video 播放器元素。");
        errorMessage.value = "未找到 video 播放器元素。";
    }
  } else if (videoPlayer.value && videoPlayer.value.canPlayType('application/vnd.apple.mpegurl')) {
    // 如果浏览器不支持 HLS.js，但支持原生 HLS (例如 Safari)
    console.log('使用原生 HLS 支持。');
    videoPlayer.value.src = props.hlsSrc;
    videoPlayer.value.addEventListener('loadedmetadata', () => {
      if (props.autoplay) {
        videoPlayer.value.play().catch(error => {
          console.warn('自动播放被阻止 (原生 HLS):', error);
          errorMessage.value = '自动播放被阻止。请点击播放按钮开始。';
        });
      }
    });
    videoPlayer.value.addEventListener('error', (e) => {
        console.error('原生 HLS 播放错误:', e);
        errorMessage.value = '使用原生 HLS 播放视频时出错。';
    });
  } else {
    // 如果浏览器既不支持 HLS.js 也不支持原生 HLS
    console.error('HLS.js 在此浏览器中不受支持，并且原生 HLS 也不可用。');
    errorMessage.value = '抱歉，您的浏览器不支持 HLS 视频播放。';
  }
};

// 销毁播放器函数
const destroyPlayer = () => {
  if (hls) {
    hls.destroy();
    hls = null;
  }
  if (videoPlayer.value) {
      // 如果之前使用的是原生HLS，清理相关事件监听器和src
      videoPlayer.value.src = ''; // 清空视频源
  }
};

// Vue 生命周期钩子：组件挂载后
onMounted(() => {
  if (props.hlsSrc && videoPlayer.value) { // 如果有 HLS 源并且 video 元素存在
    initPlayer(); // 初始化播放器
  }
});

// Vue 生命周期钩子：组件卸载前
onBeforeUnmount(() => {
  destroyPlayer(); // 销毁播放器，释放资源
});

// 监听 hlsSrc prop 的变化
watch(() => props.hlsSrc, (newSrc, oldSrc) => {
  if (newSrc && newSrc !== oldSrc && videoPlayer.value) { // 如果新源有效、与旧源不同且 video 元素存在
    console.log(`HLS 源从 ${oldSrc} 更改为 ${newSrc}`);
    errorMessage.value = ''; // 清除因旧源可能产生的错误
    destroyPlayer(); // 销毁旧的播放器实例
    initPlayer();    // 使用新源初始化播放器
  } else if (!newSrc) { // 如果没有提供新源
    destroyPlayer(); // 销毁播放器
    errorMessage.value = '未提供视频源。';
  }
});

// 如果需要从父组件控制播放器，可以暴露方法
// defineExpose({
//   play: () => videoPlayer.value?.play(),
//   pause: () => videoPlayer.value?.pause(),
// });

</script>

<style scoped>
.video-container {
  width: 100%;
  /* max-width: 800px; */ /* 示例：最大宽度 */
  position: relative; /* 用于错误消息的相对定位（如果需要更复杂的布局） */
}

.video-player {
  width: 100%;
  height: auto; /* 高度自适应 */
  display: block; /* 消除 video 元素下方可能的空白 */
  background-color: #000; /* 视频未加载时的背景色或用于信箱模式 */
}

.error-message {
  color: red; /* 错误信息文字颜色 */
  padding: 10px; /* 内边距 */
  text-align: center; /* 文字居中 */
  border: 1px solid red; /* 边框 */
  margin-top: 10px; /* 与视频播放器的间距 */
  background-color: #ffe0e0; /* 背景色 */
}
</style>
