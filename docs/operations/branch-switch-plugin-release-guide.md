# 分支切换插件发布与验证手册

## 适用范围

本文档用于维护 `check-branch-idea-plugin` 的本地验证、插件构建、发布前检查和发布操作。项目面向 IntelliJ IDEA 2024.2+，构建 JDK 为 21。

## 环境要求

- JDK 21
- Gradle Wrapper
- Git
- IntelliJ IDEA 2024.2+ 用于手动验收

Windows PowerShell 推荐先设置 JDK：

```powershell
$env:JAVA_HOME="D:\java\jdk\jdk21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## 本地验证

执行自动化测试：

```powershell
.\gradlew.bat test --stacktrace --no-configuration-cache
```

构建插件 ZIP：

```powershell
.\gradlew.bat buildPlugin --stacktrace --no-configuration-cache
```

运行沙箱 IDE 手动验证：

```powershell
.\gradlew.bat runIde
```

插件验证：

```powershell
.\gradlew.bat verifyPlugin --stacktrace --no-configuration-cache
```

构建产物默认位于：

- `build/distributions/*.zip`

## 发布前手动验收清单

- `test` 通过。
- `buildPlugin` 通过并生成 ZIP。
- `README.md` 中 `<!-- Plugin description -->` 片段与实际功能一致。
- `build.gradle.kts` 的 `pluginConfiguration.description` 仍从 README 标记区间读取。
- `src/main/resources/META-INF/plugin.xml` 不手写 `<description>`。
- `docs/operations/branch-switch-plugin-user-guide.md` 已覆盖最新用户操作。
- 使用 `runIde` 打开至少一个多仓库工作区，验证刷新不会卡死 UI。
- 验证当前已在目标分支的仓库会跳过。
- 验证目标分支远端存在、本地存在、目标缺失回退主分支三类路径。
- 验证存在已跟踪变更时，`使用 IDEA 搁置变更` 会生成 Shelf 记录并提示手动恢复。
- 验证失败卡片悬浮时能展示完整失败原因。

## 插件描述维护规则

当前项目只维护一处面向外部用户的描述来源：

- `README.md` 的 `<!-- Plugin description -->` 区块。

`build.gradle.kts` 中 `intellijPlatform.pluginConfiguration.description` 会读取该区块并写入最终插件产物。`src/main/resources/META-INF/plugin.xml` 不再手写 `<description>`，避免 GitHub、构建产物和 IDE 插件详情页出现重复维护。

功能行为发生变化时，只需要维护 `README.md` 的 `<!-- Plugin description -->` 区块。

## GitHub Release 建议流程

1. 确保本地验证通过。
2. 更新版本号，例如 `build.gradle.kts` 中的 `version`。
3. 更新 README、用户手册和发布手册。
4. 创建标签：

```powershell
git tag v0.1.0
git push origin v0.1.0
```

5. 使用 GitHub Actions 发布流程上传 `build/distributions/*.zip`。

如果后续新增 JetBrains Marketplace 发布，需要额外维护：

- 插件签名证书环境变量。
- Marketplace 发布 token。
- changelog 或 release notes。
- 插件验证矩阵。

## 当前状态

- `test` 已作为主要自动化验证入口。
- `buildPlugin` 可生成标准插件 ZIP。
- `verifyPlugin` 应继续作为正式发布前门禁；如果遇到 `:intellijPluginVerifierIdes` 依赖解析阶段异常，需要优先定位 Gradle / IntelliJ Plugin Verifier 兼容链路，而不是直接忽略插件验证。

## 排障建议

### 构建找不到 JDK 21

确认 `JAVA_HOME` 指向 JDK 21，且 `java -version` 输出为 21。

### 插件安装后看不到工具窗口

确认：

- 插件已经启用。
- 当前 IDE 版本为 2024.2+。
- `plugin.xml` 中 `toolWindow id="分支切换"` 正常加载。
- IDEA 日志中没有插件初始化异常。

### Shelf 功能执行失败

优先验证：

- Git 仓库是否已被 IDEA 识别。
- ChangeList 中是否存在该仓库范围内的已跟踪变更。
- 仓库是否处于合并、变基、冲突等阻塞状态。
- IDEA Shelf 面板是否能手动搁置同一批变更。

### GitHub Release 没有产物

确认发布 workflow 上传的是 `build/distributions/*.zip`，而不是 Marketplace 发布任务的输出。
