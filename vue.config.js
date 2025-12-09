const path = require('path');

module.exports = {
  // Thư mục build ra production
  outputDir: path.resolve(__dirname, 'src/main/resources/static'),
  assetsDir: 'js',           // JS/CSS sẽ vào static/js
  indexPath: 'index.html',   // index.html nằm trong static root

  // Cấu hình dev server
  devServer: {
    host: '0.0.0.0',   // listen on all interfaces so hoadon.vn (mapped to 127.0.0.1) is accepted
    port: 8080,            // move frontend dev server to 8080 to avoid conflict with Tomcat 8080
    hot: true,           // bật hot reload
    watchFiles: ['src/**/*'], // theo dõi tất cả file trong src
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
	historyApiFallback: true,
    // Accept requests for external hostnames (e.g. hoadon.vn) when using host mapping
    // This prevents host header rejection which can lead to ERR_CONNECTION_REFUSED in some setups
    allowedHosts: 'all',
    // Proxy API calls to backend server on 8081
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // keep path as-is
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