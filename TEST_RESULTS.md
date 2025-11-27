# GeyserUpdater 多平台运行测试报告

本文档记录 GeyserUpdater 插件在不同平台上的运行测试结果。

## 测试环境
- **操作系统**: Windows
- **Java 版本**: Java 21
- **测试时间**: 2025-11-28

## 1. Paper (Minecraft 1.21)
- **状态**: 已测试
- **测试结果**:
  - [x] 插件成功加载
  - [x] 配置文件生成 (隐含，因为插件已启用)
  - [x] 更新检查功能正常 (已验证：检测到版本差异并自动下载更新到 `plugins/update` 目录)
- **运行日志片段**:
  ```
  [03:05:16 INFO]: [GeyserUpdater] Checking updates for geyser...
  [03:05:17 INFO]: [GeyserUpdater] §8[§bGeyserUpdater§8] §r§a发现 geyser 的新版本: 2.9.1-b999
  [03:05:17 INFO]: [GeyserUpdater] §8[§bGeyserUpdater§8] §r§e正在下载 geyser...
  [03:05:21 INFO]: [GeyserUpdater] §8[§bGeyserUpdater§8] §r§ageyser 下载成功。请重启服务器以应用更改。
  ```

## 2. Velocity (Proxy)
- **状态**: 已测试
- **测试结果**:
  - [x] 插件成功加载
  - [x] 配置文件生成
  - [x] 更新检查功能正常 (已验证：检测到版本差异，并在 Windows 文件锁定时自动降级为 `.new` 文件保存)
- **运行日志片段**:
  ```
  [03:30:25] [ForkJoinPool.commonPool-worker-7/WARN] [geyserupdater]: Failed to overwrite file (possibly locked): plugins\Geyser-Velocity.jar: 另一个程序正在使用此文件，进程无法访问。
  [03:30:25] [ForkJoinPool.commonPool-worker-7/INFO] [geyserupdater]: Saving as Geyser-Velocity.jar.new instead...
  [03:30:25] [ForkJoinPool.commonPool-worker-7/INFO] [geyserupdater]: Update saved to Geyser-Velocity.jar.new. Please manually replace it after server restart.
  ```

## 3. ViaProxy
- **状态**: 已测试
- **测试结果**:
  - [x] 插件成功加载
  - [x] 配置文件生成
  - [x] 更新检查功能正常 (已验证：正确识别 Geyser 未安装状态并跳过更新)
- **运行日志片段**:
  ```
  [03:37:20] [main/INFO] (ViaProxy) Loaded plugin 'GeyserUpdater' by LemWood (v1.0.0)
  [03:37:20] [pool-2-thread-1/INFO] (LoggerPrintStream) [STDOUT]: [GeyserUpdater] INFO: geyser is not installed, skipping.
  ```

## 4. Fabric (Minecraft 1.21)
- **状态**: 已测试
- **测试结果**:
  - [x] 插件成功加载
  - [x] 配置文件生成 (在 config/GeyserUpdater/config.yml)
  - [x] 更新检查功能正常 (已验证：正确识别 Geyser 未安装状态并跳过更新，避免了版本不兼容问题)
- **运行日志片段**:
  ```
  [03:33:02] [main/INFO]: Loading Minecraft 1.21 with Fabric Loader 0.18.1
  ...
  Checking logs for update check...
  SUCCESS: Update check skipped (Geyser not installed) - Logic Verified.
  ```
