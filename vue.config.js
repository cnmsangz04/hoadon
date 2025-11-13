const path = require('path');

module.exports = {
  outputDir: path.resolve(__dirname, 'src/main/resources/static'), // Build ra static root
  assetsDir: 'js', // JS/CSS vào static/js
  indexPath: 'index.html', // index.html nằm trong static root
  devServer: {
    host: 'localhost', // localhost để HMR hoạt động
    port: 8080,
    hot: true, // bật hot reload
    watchFiles: ['src/**/*'], // theo dõi tất cả file trong src
    headers: {
      'Access-Control-Allow-Origin': '*', // cho phép CORS
    }
  }
};
