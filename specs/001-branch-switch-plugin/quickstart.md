# 快速开始：IDEA 多项目分支快速切换

## 环境要求

- JDK 21
- Gradle Wrapper
- IntelliJ IDEA 2024.2 或更高版本
- Windows PowerShell

## 本地构建

```powershell
.\gradlew.bat buildPlugin
```

预期结果：

- 生成可安装插件包。
- 通过 Java 编译。
- 所有新增或修改文件保持 UTF-8（非 BOM）。

## 本地运行插件

```powershell
.\gradlew.bat runIde
```

验证重点：

- IDEA 启动后右侧出现“分支切换”工具窗口。
- 工具窗口用户可见文案均为中文。
- 打开包含多个 Git 仓库的项目后，仓库列表可以刷新。
- 目标分支输入为空时不能执行切换，并显示中文提示。

## 手动功能验证

准备三个测试仓库：

- 仓库 A：存在目标分支。
- 仓库 B：不存在目标分支，但存在 `main` 或 `master`。
- 仓库 C：存在未提交变更。

验证步骤：

1. 在 IDEA 中打开包含三个仓库的工作区。
2. 打开右侧“分支切换”工具窗口。
3. 输入目标分支。
4. 选择三个仓库。
5. 执行切换。
6. 对仓库 C 选择临时保存变更。
7. 查看结果区。

预期结果：

- 仓库 A 切换到目标分支。
- 仓库 B 回退到主分支并展示回退状态。
- 仓库 C 在切换前保存变更，并提示需要手动恢复。
- 所有结果、失败原因和恢复提示均为中文。

## 发布验证

```powershell
.\gradlew.bat test
.\gradlew.bat buildPlugin
.\gradlew.bat verifyPlugin
```

发布流水线要求：

- 推送和拉取请求执行构建、测试和插件验证。
- `v*` 标签触发发布流程。
- 发布流程上传插件安装包。

## 文档同步检查

实施过程中如修改功能行为、界面文案、分支策略或发布流程，必须同步更新：

- `docs/features/branch-switch-plugin.md`
- `docs/decisions/idea-plugin-tech-stack.md`
- 当前 feature 目录下的规格、计划或任务文档
