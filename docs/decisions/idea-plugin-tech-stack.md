# IDEA 插件技术选型

**日期**: 2026-06-03

## 结论

当前项目使用 Java 21、Gradle Kotlin DSL、Gradle 9.0.0、IntelliJ Platform Gradle Plugin 2.16.0，
并以 IntelliJ IDEA 2024.2+ 作为初始兼容基线。

## 参考项目

- `E:\workspace\project\mt-tool` 使用 Java 17、`org.jetbrains.intellij` 1.16.1、IntelliJ IDEA 2023.1.5，
  并通过右侧 `toolWindow` 承载插件主界面。
- `E:\workspace\project\easy-pojo2json` 使用 Java 17、`org.jetbrains.intellij.platform` 2.0.1、
  IntelliJ IDEA 2023.3，并配置了插件描述、变更日志、签名、发布、验证和独立安装包构建。
- `E:\workspace\project\deep-read-web\.github` 提供了 Windows 持续集成、手动发布、标签发布、
  构建产物上传和 GitHub 发布创建流程，可作为本项目发布流水线的结构参考。

## Java 21 选择

本项目明确采用 Java 21：

- 用户已指定使用 Java 21，后续工程模板、源码编译目标和 CI 都应围绕 Java 21 设置。
- IDEA 2024.2 起默认随 IDE 使用 JBR21，更适合作为 Java 21 插件的运行基线。
- 分支切换插件属于新项目，不需要为了兼容更旧 IDE 版本保留 Java 17 编译目标。
- 使用 Java 21 可以让后续代码使用更现代的 JDK 能力，同时保持技术栈简洁。

采用 Java 21 的代价：

- 初始插件兼容范围将收敛到 IDEA 2024.2+，不承诺支持 2023.3 或更早版本。
- 持续集成、发布和本地开发都需要安装 JDK 21。
- GitHub Actions 线上允许下载 verifier 所需 IDE；本机插件兼容性验证只允许使用已安装 IDEA。

## 推荐技术栈

- 语言：Java 21
- 构建：Gradle Kotlin DSL、Gradle Wrapper 9.0.0
- 插件构建：IntelliJ Platform Gradle Plugin 2.16.0
- IDE 基线：IntelliJ IDEA Community 2024.2+
- 插件依赖：平台能力、Java 模块、Git 相关能力
- 界面入口：右侧工具窗口，默认支持固定停靠使用方式
- 持久化：优先使用 IDEA 平台状态服务保存用户偏好，例如主分支候选、最近目标分支和默认暂存策略
- 打包：生成标准插件 ZIP，同时可保留独立安装包产物
- 发布：持续集成测试、插件验证、插件构建产物、标签触发 GitHub 发布

## 发布流水线建议

- `ci.yml` 在推送和拉取请求时执行测试和插件构建。
- `release.yml` 支持手动触发和 `v*` 标签触发。
- GitHub Actions 使用 JDK 21 测试、打包和执行线上插件验证。
- 发布流程需要上传插件 ZIP 和可选独立安装包。
- `verifyPlugin` 进入 CI 和 GitHub Release 默认链路；线上允许下载 verifier 所需 IDE。
- 本机执行 `verifyPlugin` 时必须显式指定本机已有 IDEA 路径，避免自动下载占满磁盘。
- 如果后续要发布到 JetBrains Marketplace，需要通过环境变量提供签名证书和发布 token。

## 当前实际构建命令

当前仓库已验证或约定的命令如下：

- 单元与集成测试：

```powershell
.\gradlew.bat test --stacktrace --no-configuration-cache
```

- 构建插件 ZIP：

```powershell
.\gradlew.bat buildPlugin --stacktrace --no-configuration-cache
```

- 本机插件兼容性验证。必须指定本机已安装 IDEA 路径，避免下载大量 IDE：

```powershell
.\gradlew.bat verifyPlugin -PlocalVerificationIdePath="D:\java\idea\IntelliJ IDEA 2025.3.1.1" --stacktrace --no-configuration-cache
```

- 沙箱运行：

```powershell
.\gradlew.bat runIde
```

当前状态说明：

- `test` 已通过。
- `buildPlugin` 已通过，并生成插件 ZIP。
- `verifyPlugin` 已恢复到 CI / Release 链路；本机只作为显式验证入口保留。

## 暂不采用的方案

- 暂不引入 Kotlin 作为主开发语言，当前项目需求用 Java 更贴近参考项目和既有代码风格。
- 暂不做独立后台服务或外部命令行工具，优先使用 IDEA 内置项目和 Git 能力。

## 参考资料

- [JetBrains IntelliJ IDEA 2024.2 发布说明](https://www.jetbrains.com/idea/whatsnew/2024-2/)：
  2024.2 起 IDE 更新随 JBR21 提供。
- [IntelliJ Platform Gradle Plugin 2.x 文档](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html?from=jetbrains.org)：
  插件构建体系使用 `org.jetbrains.intellij.platform`。
- [JetBrains IntelliJ Project Migrates to Java 17](https://blog.jetbrains.com/platform/2022/08/intellij-project-migrates-to-java-17/)：
  可作为理解较早平台 Java 基线的背景资料。
