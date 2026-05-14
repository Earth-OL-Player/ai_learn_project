import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// Vite 配置保持轻量，方便本地快速启动。
export default defineConfig({
  plugins: [vue()],
  cacheDir: '.vite-cache',
  server: {
    port: 5173,
  },
});
