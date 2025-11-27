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
  - [ ] 更新检查功能正常
- **运行日志片段**:
  ```
  [02:39:13 INFO]: Loading plugins...
  [02:39:13 INFO]: Loaded plugin geyserupdater 1.0.0 by lemwood
  [02:39:13 INFO]: Loaded 2 plugins
  [02:39:14 INFO]: Done (1.3s)!
  ```

## 3. ViaProxy
- **状态**: 已测试
- **测试结果**:
  - [x] 插件成功加载
  - [x] 配置文件生成
  - [ ] 更新检查功能正常
- **运行日志片段**:
  ```
  [02:42:03] [main/INFO] (ViaProxy) Loaded plugin 'GeyserUpdater' by LemWood (v1.0.0)
  [02:42:04] [main/INFO] (ViaProxy) Enabled plugin 'GeyserUpdater'
  ```

## 4. Fabric (Minecraft 1.21)
- **状态**: 已测试
- **测试结果**:
  - [x] 插件成功加载
  - [x] 配置文件生成 (在 config/GeyserUpdater/config.yml)
  - [ ] 更新检查功能正常 (日志未显示详细更新检查信息，需进一步确认)
- **运行日志片段**:
  ```
  [03:00:16] [main/INFO]: Loading 42 mods:
	...
	- geyserupdater 1.0.0
	...
  [03:00:37] [Server thread/INFO]: Done (9.101s)! For help, type "help"
  ```
