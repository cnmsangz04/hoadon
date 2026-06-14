const path = require('path');

module.exports = {
  // Thư mục build ra production
  outputDir: path.resolve(__dirname, 'src/main/resources/static'),
  assetsDir: 'js',           // JS/CSS sẽ vào static/js
  indexPath: 'index.html',   // index.html nằm trong static root

  // Cấu hình dev server
  devServer: {
    host: '0.0.0.0',   // lắng nghe trên mọi interface để chấp nhận hoadon.vn (map về 127.0.0.1)
    port: 8080,            // chuyển dev server frontend sang 8080 để tránh trùng với Tomcat 8080
    hot: true,           // bật hot reload
    watchFiles: ['src/**/*'], // theo dõi tất cả file trong src
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
	historyApiFallback: true,
    // Chấp nhận request từ hostname bên ngoài (ví dụ hoadon.vn) khi dùng host mapping
    // Tránh bị từ chối host header gây ERR_CONNECTION_REFUSED trong một số môi trường
    allowedHosts: 'all',
    // Proxy các lệnh gọi API sang backend server ở cổng 8081
    proxy: {
      '/v1': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // giữ nguyên path
        secure: false,
        ws: false,
        logLevel: 'warn'
      },
      // Proxy file upload tĩnh (avatar, v.v.) sang backend để tránh CORS khi dev
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        ws: false,
        logLevel: 'warn'
      }
    }
  },

  // Cấu hình Webpack (tuỳ chọn)
  configureWebpack: {
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      }
    }
  }
};
