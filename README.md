# check-branch-idea-plugin

这是一个 IntelliJ IDEA 插件项目，用于在多个项目之间统一、快速、安全地切换 Git 分支。

<!-- Plugin description -->
## 插件简介

该插件用于在一个 IDEA 工作区内快速查看多个 Git 仓库状态，并将选中的仓库统一切换到指定分支。

首版目标：

- 在右侧工具窗口展示当前工作区仓库列表。
- 支持批量切换和单仓库切换。
- 在目标分支缺失时自动回退到主分支。
- 在切换前优先使用 IDEA 搁置变更保护未提交代码，必要时降级为 Git 暂存栈。
- 用中文展示切换结果、失败原因和手动恢复提示。
<!-- Plugin description end -->

## 本地开发

环境要求：

- JDK 21
- IntelliJ IDEA 2024.2 或更高版本

常用命令：

```powershell
.\gradlew.bat test
.\gradlew.bat buildPlugin
.\gradlew.bat verifyPlugin
.\gradlew.bat runIde
```

构建产物默认输出到 `build/distributions/`。

## 工程入口

- Gradle 构建：`build.gradle.kts`
- 插件描述：`src/main/resources/META-INF/plugin.xml`
- 中文资源：`src/main/resources/messages/CheckBranchBundle.properties`
- 持续集成：`.github/workflows/ci.yml`
- 标签发布：`.github/workflows/release.yml`

## 文档

项目知识库维护在 [docs/README.md](docs/README.md)。任何功能、行为变更、实现细节或未来待办，
都需要在代码变更的同一轮工作中同步到文档。
