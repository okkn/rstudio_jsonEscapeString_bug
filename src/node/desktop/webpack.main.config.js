const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');
const copyWebpackPlugin = new CopyWebpackPlugin({
  patterns: [
    {
      from: path.resolve(__dirname, 'src', 'assets'),
      to: path.resolve(__dirname, '.webpack', 'main', 'assets'),
    },
    {
      from: path.resolve(__dirname, '.webpack', 'main', 'native_modules'),
      to: path.resolve(__dirname, '.webpack', 'main'),
    },
  ],
});

const relocateLoader = require('@vercel/webpack-asset-relocator-loader');
const webpack = require('webpack');
const nodeExternals = require('webpack-node-externals');

module.exports = {
  /**
   * This is the main entry point for your application, it's the first file
   * that runs in the main process.
   */
  entry: './src/main/main.ts',
  // Put your normal webpack config below here
  module: {
    rules: require('./webpack.rules'),
  },
  resolve: {
    extensions: ['.js', '.ts', '.jsx', '.tsx', '.css', '.json'],
    modules: ['node_modules'],
  },
  plugins: [copyWebpackPlugin, 
    {
    apply(compiler) {
      compiler.hooks.compilation.tap(
        'webpack-asset-relocator-loader',
        (compilation) => {
          relocateLoader.initAssetCache(compilation, '');
        },
      );
    },
  }
  ],
  externals: [nodeExternals()],
  externalsPresets: { node: true },
};
