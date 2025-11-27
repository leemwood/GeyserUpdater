# GeyserUpdater

GeyserUpdater 是一个用于自动检查并更新 Geyser 插件的工具，支持多种 Minecraft 服务端和代理平台。

## 支持平台

- **Paper** (Minecraft 1.21+)
- **Velocity**
- **ViaProxy**
- **Fabric**

## 功能特性

- 自动检测 Geyser 新版本
- 自动下载更新
- 多平台支持
- 国际化支持 (中文/英文)

## 构建项目

使用 Gradle 构建项目：

```bash
./gradlew clean collectJars
```

构建产物将生成在 `dist` 目录下。

## 配置文件

插件启动后会在相应配置目录下生成 `config.yml`，支持自定义更新检查频率、下载源等设置。
