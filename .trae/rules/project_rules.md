# Project Rules

## 构建与开发
- **Fabric ShadowJar**: 在配置 Fabric 项目的 ShadowJar 时，必须排除 Minecraft、Fabric Loader 及 Fabric API 等环境依赖，或使用独立的 `shadow` configuration 仅包含需要打包的库，以避免类加载冲突（如 `MixinExtras` 冲突）。
- **构建命令**: ./gradlew collectJars 用于收集所有必要的 jar 文件到 `dist` 目录。

## 测试
- **自动化测试**: 推荐使用 PowerShell 脚本 (`test_*.ps1`) 进行各平台的自动化下载、部署和测试。
- **测试记录**: 测试结果应记录在 `TEST_RESULTS.md` 中，包括测试状态、结果和关键日志片段。
- **Headless 运行**: 服务端测试应尽量使用无头模式 (`nogui` 或类似参数) 以便于脚本自动化控制。

## 国际化
- **中文优先**: 配置文件和消息文件应包含中文注释和默认中文内容。

## 更新机制
- **文件锁定处理**: 在 Windows 等平台遇到文件锁定无法替换时，应生成 `.new` 文件，并根据配置生成关闭后执行的脚本（.bat/.sh）来完成替换。
- **面板服兼容性**: 在配置文件中提供 `enable-shutdown-script` 选项，并注明在部分面板服（如 Pterodactyl）可能因容器立即停止而失效的警告。

## Geyser Extension 支持
- **独立版限制**: Geyser Extension 模块 (`geyser-extension`) 仅在 Geyser Standalone 模式下运行，其他模式会自动禁用。
- **项目过滤**: 在 Extension 模式下，默认禁用 Floodgate 检查（因通常无需独立 jar）。
- **路径处理**: Extension 模式下，Geyser 核心更新至根目录，Extension 更新至 `extensions` 目录。
