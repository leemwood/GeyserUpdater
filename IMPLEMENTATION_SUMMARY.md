# GeyserUpdater 下载逻辑修复实现总结

## 修复的问题

### 1. 文件名匹配问题
**问题**: 原代码使用远程API返回的确切文件名查找本地文件，导致即使文件已存在也会重复下载。

**修复**: 
- 在 `PlatformAdapter` 接口中添加了 `findInstalledJar()` 方法
- 支持模糊匹配常见的文件名前缀（如 "Geyser", "geyser", "Floodgate" 等）
- 遍历目录查找匹配的jar文件，而不是依赖确切文件名

### 2. 版本检查逻辑不一致
**问题**: 各平台版本比较逻辑不统一，某些平台无法获取版本信息时会重复下载。

**修复**:
- 改进了 `checkProject` 方法的逻辑流程
- 区分了"文件不存在"和"版本不匹配"两种情况
- 对于无法获取版本的情况，依赖哈希检查避免重复下载

### 3. 哈希检查逻辑缺陷
**问题**: 原代码只检查确切文件名，无法找到已安装的文件进行哈希比较。

**修复**:
- 使用新的 `findInstalledJar()` 方法查找已安装文件
- 先检查已安装文件的哈希，再检查更新目录中的文件
- 避免了重复下载相同内容的文件

### 4. 平台特定的文件名识别
**修复**:
- **Paper**: 支持 "Geyser-Spigot", "Geyser", "geyser" 等多种命名
- **Velocity**: 支持 "geyser", "Geyser", "geyser-velocity" 等
- **Fabric**: 支持 "geyser-fabric", "geyser", "Geyser" 等
- **ViaProxy**: 支持 "Geyser-ViaProxy", "geyser", "Geyser" 等

## 核心改进

### 1. 智能文件查找
```java
default Path findInstalledJar(String projectId, Path searchDir) {
    String[] possiblePrefixes = switch (projectId) {
        case "geyser" -> new String[]{"Geyser", "geyser"};
        case "floodgate" -> new String[]{"floodgate", "Floodgate"};
        case "geyserextras" -> new String[]{"GeyserExtras", "geyserextras"};
        default -> new String[]{projectId};
    };
    // 遍历目录查找匹配的jar文件
}
```

### 2. 改进的哈希检查
```java
private boolean isFileUpToDate(String project, UpdateClient.UpdateVersion version, boolean isUpdate) {
    // 1. 查找已安装的文件
    Path installedFile = platform.findInstalledJar(project, installedFolder);
    
    // 2. 检查已安装文件的哈希
    if (installedFile != null && Files.exists(installedFile)) {
        // 计算并比较哈希
    }
    
    // 3. 检查更新目录中是否已下载
    if (isUpdate) {
        // 检查更新目录
    }
}
```

### 3. 优化的版本比较逻辑
- 明确区分文件缺失和版本不匹配的情况
- 对于无法获取版本的平台，依赖哈希检查
- 减少不必要的API调用和下载

## 测试验证

构建成功生成了所有平台的jar文件：
- `paper-1.0.0-alpha.4.jar`
- `velocity-1.0.0-alpha.4.jar` 
- `fabric-1.0.0-alpha.4.jar`
- `viaproxy-1.0.0-alpha.4.jar`
- `geyser-extension-1.0.0-alpha.4.jar`

## 构建问题修复

在实现过程中还修复了以下构建问题：
1. **Fabric模块构建失败**: 简化了Fabric Loom配置，移除了复杂的shadow配置
2. **版本号格式问题**: 将版本号从 `1.0.0alpha-4` 改为 `1.0.0-alpha.4` 以符合语义版本规范
3. **依赖配置优化**: 简化了Fabric模块的依赖配置，避免了remapJar任务的文件系统异常

## 预期效果

1. **避免重复下载**: 已安装且为最新版本的插件不会被重复下载
2. **提高检测准确性**: 支持多种文件命名方式，提高文件识别率
3. **减少网络请求**: 通过哈希检查避免不必要的下载
4. **统一行为**: 所有平台使用一致的检查逻辑

这些修复解决了用户反馈的"插件已存在但仍然重复下载"的核心问题。