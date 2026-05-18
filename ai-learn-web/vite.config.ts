import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// 允许 cpolar 免费隧道域名访问本地 Vite 开发服务。
const CPOLAR_ALLOWED_HOST = '.cpolar.cn';

// 后端服务只在本机访问，由 Vite 代理转发给朋友的公网请求。
const LOCAL_BACKEND_TARGET = 'http://localhost:8080';

// Vite 配置保持轻量，方便本地快速启动。
export default defineConfig({
  plugins: [vue()],
  cacheDir: '.vite-cache',
  server: {
    port: 5173,
    // 仅放行 cpolar 隧道域名，避免直接关闭 Vite 的 Host 安全校验。
    allowedHosts: [CPOLAR_ALLOWED_HOST],
    proxy: {
      // 统一把公网访问中的 /api 请求代理到本机后端，避免浏览器请求朋友电脑的 localhost。
      '/api': {
        target: LOCAL_BACKEND_TARGET,
        changeOrigin: true,
      },
    },
  },
});
