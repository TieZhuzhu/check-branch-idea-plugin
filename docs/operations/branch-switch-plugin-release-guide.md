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

本地插件兼容性验证只允许使用本机已经安装的 IDEA。必须显式指定本机 IDEA 安装目录，不能让
Plugin Verifier 自动下载 IDE：

```powershell
.\gradlew.bat verifyPlugin -PlocalVerificationIdePath="D:\java\idea\IntelliJ IDEA 2025.3.1.1" --stacktrace --no-configuration-cache
```

构建产物默认位于：

- `build/distributions/*.zip`

## 发布前手动验收清单

- `test` 通过。
- `buildPlugin` 通过并生成 ZIP。
- 本机如需插件兼容性验证，必须使用 `localVerificationIdePath` 指向本机已有 IDEA，不允许下载大量 IDE。
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

1. 确保本地测试和插件构建通过。
2. 更新版本号，例如 `build.gradle.kts` 中的 `version`。
3. 更新 README、用户手册和发布手册。
4. 创建标签：

```powershell
git tag v0.1.0
git push origin v0.1.0
```

5. 使用 GitHub Actions 发布流程执行测试、构建、线上插件验证并上传 `build/distributions/*.zip`。

如果后续新增 JetBrains Marketplace 发布，需要额外维护：

- 插件签名证书环境变量。
- Marketplace 发布 token。
- changelog 或 release notes。
- GitHub Actions 线上插件验证策略，以及本机 IDEA 安装目录下的插件兼容性验证策略。

## 当前状态

- `test` 已作为 CI 和发布流程的主要自动化验证入口。
- `buildPlugin` 可生成标准插件 ZIP，并作为 GitHub Release 产物来源。
- `verifyPlugin` 已进入 CI 和 GitHub Release 默认链路；GitHub Actions 线上允许下载 verifier 所需 IDE。
- 本地直接运行 `verifyPlugin` 时不会配置任何下载型 IDE；本机验证必须通过 `localVerificationIdePath` 指向已有 IDEA。

### Plugin Verifier 下载策略

GitHub Actions 线上允许下载 verifier 所需 IDE，用于保证 CI 和 Release 仍保留插件兼容性验证。
本机不允许默认下载大量 IDE，避免 Gradle 缓存占满磁盘。

本机需要做兼容性验证时，先确认目标 IDEA 已经安装，再执行：

```powershell
.\gradlew.bat verifyPlugin -PlocalVerificationIdePath="D:\java\idea\IntelliJ IDEA 2025.3.1.1" --stacktrace --no-configuration-cache
```

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
