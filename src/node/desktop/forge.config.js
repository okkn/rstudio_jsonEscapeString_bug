const config = {
  makers: [
    {
      name: '@electron-forge/maker-squirrel',
      config: {
        name: 'rstudio',
      },
    },
    {
      name: '@electron-forge/maker-zip',
      platforms: ['linux', 'win32'],
    },
    {
      name: '@electron-forge/maker-deb',
      config: {},
    },
    {
      name: '@electron-forge/maker-rpm',
      config: {},
    },
  ],

  plugins: [
    ['@electron-forge/plugin-auto-unpack-natives'],
    [
      '@electron-forge/plugin-webpack',
      {
        mainConfig: './webpack.main.config.js',
        renderer: {
          config: './webpack.renderer.config.js',
          entryPoints: [
            {
              js: './src/renderer/renderer.ts',
              name: 'main_window',
              preload: {
                js: './src/renderer/preload.ts',
              },
            },
            {
              html: './src/ui/loading/loading.html',
              js: './src/ui/loading/loading.ts',
              name: 'loading_window',
            },
            {
              html: './src/ui/error/error.html',
              js: './src/ui/error/error.ts',
              name: 'error_window',
            },
            {
              html: './src/ui/connect/connect.html',
              js: './src/ui/connect/connect.ts',
              name: 'connect_window',
            },
            {
              html: './src/ui/widgets/choose-r/ui.html',
              js: './src/ui/widgets/choose-r/load.ts',
              preload: {
                js: './src/ui/widgets/choose-r/preload.ts',
              },
              name: 'choose_r',
            },
          ],
        },
      },
    ],
  ],
  packagerConfig: {
    icon: './resources/icons/RStudio',
    asar: { unpack: true }
  },
  hooks: {
    generateAssets: async () => {
      console.log('test55 generate assets');
      const fs = require('fs');
      const path = require('path');

      var copyRecursiveSync = async (src, dest) => {
        console.log('test55 copyrecursive sync ',src,dest);
        
        var exists = fs.existsSync(src);
        console.log('test55 copyrecursive sync 1');
        var stats = exists && fs.statSync(src);
        console.log('test55 copyrecursive sync 2');
        var isDirectory = exists && stats.isDirectory();
        console.log('test55 copyrecursive sync 3');
        if (isDirectory) {
          try {
            console.log('test55 create folder');

            await fs.mkdir(dest);
          } catch (err) {
            console.log('test55 create folder err');

          }
          fs.readdirSync(src).forEach(async (childItemName) => {
            await copyRecursiveSync(path.join(src, childItemName),
              path.join(dest, childItemName));
          });
        } else {
          try {
            console.log('test55 copy file');
            await fs.copyFile(src, dest);
          } catch (err) {
            console.log('test55 create file err');

          }
        }
      };
      
      console.log('test55 BEFORE copyrecursive sync');

      // await copyRecursiveSync('./.webpack/main/native_modules', './.webpack/main');
    }
  }
};

module.exports = config;
