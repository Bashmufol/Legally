import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const useProxy = env.VITE_USE_API_PROXY === 'true'
  const proxyTarget =
    env.VITE_PROXY_TARGET ||
    env.VITE_API_URL ||
    'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: useProxy
        ? {
            '/api': {
              target: proxyTarget,
              changeOrigin: true,
              secure: true,
            },
            '/actuator': {
              target: proxyTarget,
              changeOrigin: true,
              secure: true,
            },
          }
        : undefined,
    },
  }
})
