# Android-DeskPet

真人悬浮桌宠 Android 工程，已配置 GitHub Actions 自动构建 APK。

## 第一次上传到 GitHub

把项目根目录里的全部文件和文件夹上传到仓库根目录，尤其不要漏掉：

- `.github/workflows/build-apk.yml`
- `app/`
- `build.gradle`
- `settings.gradle`
- `gradle.properties`

上传后提交到 `main` 分支。

## 自动构建

提交后 GitHub 会自动运行：

`Actions → Build Android APK`

构建成功后，打开该次运行页面，在最下面的 **Artifacts** 下载：

`Android-DeskPet-APK`

解压后得到：

`Android-DeskPet-debug.apk`

## 手机安装

1. 把 APK 发到安卓手机。
2. 允许浏览器/文件管理器“安装未知应用”。
3. 安装并打开“我的桌宠”。
4. 首次点击“开启桌宠”时，允许“显示在其他应用上层”。
5. 返回 App，再点一次“开启桌宠”。

支持：悬浮、拖动、点击互动、待机/学习/看手机/睡觉。
最低 Android 8.0。
