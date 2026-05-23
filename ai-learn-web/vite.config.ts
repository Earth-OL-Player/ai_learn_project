import { defineConfig, type ServerOptions } from 'vite';
import vue from '@vitejs/plugin-vue';

// 后端服务只在本机访问，由 Vite 代理转发本地开发请求。
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

/**
 * 创建开发服务器配置。
 *
 * @return 开发服务器配置
 */
function createServerOptions(): ServerOptions {
  return {
    port: 5173,
    proxy: {
      // 统一把本地开发中的 /api 请求代理到本机后端，便于沿用同源调用路径。
      '/api': {
        target: LOCAL_BACKEND_TARGET,
        changeOrigin: true,
      },
    },
  };
}

// Vite 配置保持轻量，方便本地快速启动。
export default defineConfig(() => ({
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
  server: createServerOptions(),
}));
