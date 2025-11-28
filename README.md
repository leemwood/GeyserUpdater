# GeyserUpdater

GeyserUpdater 是一个用于自动检查并更新 Geyser 插件的工具，支持多种 Minecraft 服务端和代理平台。

## 支持平台

- **Paper** (Minecraft 1.21+)
- **Velocity**
- **ViaProxy**
- **Fabric**
- **Geyser Standalone** (作为 Extension 运行)

## 功能特性

- **自动更新**: 自动检测 Geyser、Floodgate 及 GeyserExtras 的新版本并下载。
- **自动重启**: 支持在更新完成后自动重启服务器（需配置启动脚本）。
- **文件锁定处理**: Windows 平台下自动处理文件锁定问题，生成更新脚本在关闭时替换。
- **多平台支持**: 统一的核心逻辑适配主流服务端与代理。
- **国际化支持**: 所有提示消息均可在 `messages.yml` 中自定义（默认中文）。

## 构建项目

使用 Gradle 构建项目：

```bash
./gradlew clean collectJars
```

构建产物将生成在 `dist` 目录下，包含所有平台的 jar 文件。

## 安装与使用

1.  下载对应平台的 jar 文件。
    *   **Paper/Velocity/ViaProxy**: 放入 `plugins` 目录。
    *   **Fabric**: 放入 `mods` 目录。
    *   **Geyser Standalone**: 放入 `extensions` 目录。
2.  启动服务器，插件会自动生成配置文件。
3.  根据需要修改 `config.yml` 和 `messages.yml`。

## 配置文件

插件启动后会在相应配置目录下生成 `config.yml`，主要配置项包括：

*   `update-strategy`: 更新策略 (AUTO/MANUAL/CHECK_ONLY)。
*   `auto-install`: 是否自动安装缺失的项目。
*   `auto-restart`: 自动重启设置（触发项目、延迟、启动脚本路径）。
*   `enable-shutdown-script`: 是否启用关闭时更新脚本（解决文件锁定）。
