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
- 刷新期间显示中文加载状态，刷新完成后显示最近刷新时间。
- 状态可能过期时，界面会提示先刷新再切换。
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
- 如果仓库 C 的 IDEA 搁置变更不可用，界面会用中文说明当前未自动启用 Git 暂存栈降级。
- 所有结果、失败原因和恢复提示均为中文。

## 当前验证记录

- 2026-06-03：已在 `JDK 21` 下执行 `.\gradlew.bat test --stacktrace --no-configuration-cache`，通过。
- 2026-06-03：已执行 `.\gradlew.bat buildPlugin --stacktrace --no-configuration-cache`，通过，产物为 `build/distributions/check-branch-idea-plugin-0.1.0-SNAPSHOT.zip`。
- 2026-06-03：已执行 `.\gradlew.bat verifyPlugin --stacktrace --no-configuration-cache`，未通过。当前阻塞为 `:intellijPluginVerifierIdes` 依赖解析阶段出现 `ConcurrentModificationException`，属于 Gradle / IntelliJ Plugin Verifier 依赖链路问题，尚未定位到业务代码错误。
- 2026-06-03：IDEA 内手动验证仍需在本机通过 `.\gradlew.bat runIde` 打开沙箱 IDE 后执行，当前会话未自动启动图形界面进行录制式验收。

## 操作文档

详细操作说明见：

- `docs/operations/branch-switch-plugin-user-guide.md`
- `docs/operations/branch-switch-plugin-release-guide.md`

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
