import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// 允许 cpolar 免费隧道域名访问本地 Vite 开发服务。
const CPOLAR_ALLOWED_HOST = '.cpolar.cn';

// 后端服务只在本机访问，由 Vite 代理转发给朋友的公网请求。
const LOCAL_BACKEND_TARGET = 'http://localhost:8080';

/**
 * 将第三方依赖拆成稳定缓存包。
 */
function resolveManualChunk(id: string): string | undefined {
  if (!id.includes('node_modules')) {
    return undefined;
  }

  // Element Plus 全量注册体积较大，按组件路径继续拆分避免单包过大。
  if (id.includes('element-plus')) {
    return 'vendor-element-plus';
  }
  if (id.includes('vue') || id.includes('pinia')) {
    return 'vendor-vue';
  }
  return 'vendor';
}

// Vite 配置保持轻量，方便本地快速启动。
export default defineConfig({
  plugins: [vue()],
  cacheDir: '.vite-cache',
  build: {
    rollupOptions: {
      output: {
        // 配合路由懒加载，避免单个 JS chunk 超过 Vite 默认 500KB 提醒阈值。
        manualChunks: resolveManualChunk,
      },
      onwarn(warning, defaultHandler) {
        // @vueuse/core 的 PURE 注释位置提示不影响运行，过滤后保持构建日志聚焦业务问题。
        if (warning.code === 'INVALID_ANNOTATION' && warning.id?.includes('@vueuse/core')) {
          return;
        }
        defaultHandler(warning);
      },
    },
  },
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
