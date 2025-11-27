# Project Rules

## 构建与开发
- **Fabric ShadowJar**: 在配置 Fabric 项目的 ShadowJar 时，必须排除 Minecraft、Fabric Loader 及 Fabric API 等环境依赖，或使用独立的 `shadow` configuration 仅包含需要打包的库，以避免类加载冲突（如 `MixinExtras` 冲突）。

## 测试
- **自动化测试**: 推荐使用 PowerShell 脚本 (`test_*.ps1`) 进行各平台的自动化下载、部署和测试。
- **测试记录**: 测试结果应记录在 `TEST_RESULTS.md` 中，包括测试状态、结果和关键日志片段。
- **Headless 运行**: 服务端测试应尽量使用无头模式 (`nogui` 或类似参数) 以便于脚本自动化控制。

## 国际化
- **中文优先**: 配置文件和消息文件应包含中文注释和默认中文内容。
