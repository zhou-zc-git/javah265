// 从 'vue' 中导入 createApp 函数，用于创建 Vue 应用实例
import { createApp } from 'vue';

// 导入根组件 App.vue
import App from './App.vue';

// 创建 Vue 应用实例，并传入 App 组件作为根组件
const app = createApp(App);

// 将 Vue 应用实例挂载到 HTML 页面上 ID 为 'app' 的元素上
// 这个 #app 元素通常在 public/index.html 文件中定义
app.mount('#app');
