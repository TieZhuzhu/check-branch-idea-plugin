# easy-multi-project-check-branch

`easy-multi-project-check-branch` is an IntelliJ IDEA plugin for checking and switching Git branches across multiple repositories in the same IDEA workspace.

`easy-multi-project-check-branch` 是一个 IntelliJ IDEA 插件，用于在一个 IDEA 工作区内查看多个 Git 仓库状态，并对选中的仓库统一切换分支。它面向微服务、多模块、多仓库协同开发场景，目标是减少逐个仓库切分支的重复操作和漏切风险。

<!-- Plugin description -->

<h2>easy-multi-project-check-branch</h2>

<p>easy-multi-project-check-branch helps you inspect and switch branches for multiple Git repositories from one IntelliJ IDEA tool window.</p>

<p>Key features:</p>

<ul>
  <li>Discovers Git repositories already recognized by the current IDEA project without scanning arbitrary disk folders.</li>
  <li>Refreshes repository status asynchronously, including current branch, target branch availability, blocked states, and tracked local changes.</li>
  <li>Supports batch switching and single-repository switching; repositories already on the target branch are skipped automatically.</li>
  <li>Prefers remote target branches, falls back to local branches when remote checkout is unavailable, and can fall back to configured main-branch candidates.</li>
  <li>Protects tracked local changes with IDEA Shelf before switching and asks users to restore shelved changes manually afterward.</li>
  <li>Keeps pure untracked files out of change protection by default to stay close to IDEA's native branch-switching behavior.</li>
  <li>Shows success, fallback, skipped, failed, and manual-restore-required result cards, with full failure reasons available on hover.</li>
</ul>

<p>中文说明：</p>

<ul>
  <li>自动发现当前 IDEA 项目中已识别的多个 Git 仓库，不扫描任意磁盘目录。</li>
  <li>异步刷新仓库状态，展示当前分支、目标分支可用性、阻塞状态和未提交变更状态。</li>
  <li>支持批量切换与单仓库精确切换，当前已经处在目标分支的仓库会自动跳过。</li>
  <li>目标分支存在于远端时优先按远端引用切换；远端不可用时降级尝试本地分支；目标分支缺失时按主分支候选回退。</li>
  <li>切换前检测已跟踪的未提交变更，优先使用 IDEA Shelf 搁置变更，切换完成后提示用户手动恢复。</li>
  <li>纯未跟踪文件默认不触发变更保护，尽量贴近 IDEA 自身切分支体验。</li>
  <li>底部结果卡片展示成功、回退、跳过、失败和待恢复状态，鼠标悬浮可查看完整失败原因。</li>
</ul>

<!-- Plugin description end -->

## 项目信息

- 插件 ID：`com.augustlee.tool.checkbranch`
- 插件名称：`easy-multi-project-check-branch`
- Gradle 根项目：`check-branch-idea-plugin`
- 主包名：`com.augustlee.tool.checkbranch`
- Java 版本：21
- IntelliJ Platform 基线：IntelliJ IDEA Community 2024.2+
- 插件依赖：Platform、Java、Git4Idea
- 许可证：MIT

## 安装与使用

从源码构建插件 ZIP：

```powershell
$env:JAVA_HOME="D:\java\jdk\jdk21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat buildPlugin
```

构建完成后，在 IDEA 中通过 `Settings | Plugins | Install Plugin from Disk...` 安装 `build/distributions/*.zip`。

基本使用流程：

1. 在 IntelliJ IDEA 2024.2+ 中打开包含多个 Git 仓库的工作区。
2. 打开右侧工具窗口 `分支切换`。
3. 在 `目标分支` 中输入分支名，例如 `feature/demo`。
4. 点击 `刷新状态`，等待进度条结束并查看仓库状态。
5. 勾选需要参与切换的仓库，默认刷新后会全选。
6. 点击 `执行切换`。
7. 如果某个仓库存在未提交变更，按弹窗选择 `使用 IDEA 搁置变更`、`跳过该仓库` 或 `取消本次操作`。
8. 切换完成后，在底部结果卡片查看逐仓库状态；如果出现 `待恢复`，请到 IDEA Shelf 中手动恢复对应搁置记录。

更完整的用户操作说明见 [branch-switch-plugin-user-guide.md](docs/operations/branch-switch-plugin-user-guide.md)。

## 本地开发

环境要求：

- JDK 21
- IntelliJ IDEA 2024.2 或更高版本
- Git 命令可在本机 PATH 中访问

常用命令：

```powershell
.\gradlew.bat test
.\gradlew.bat buildPlugin
.\gradlew.bat verifyPlugin
.\gradlew.bat runIde
```

构建产物默认输出到 `build/distributions/`。

## 工程入口

- Gradle 构建：[build.gradle.kts](build.gradle.kts)
- 插件描述：[plugin.xml](src/main/resources/META-INF/plugin.xml)
- 中文资源：[CheckBranchBundle.properties](src/main/resources/messages/CheckBranchBundle.properties)
- 用户手册：[branch-switch-plugin-user-guide.md](docs/operations/branch-switch-plugin-user-guide.md)
- 发布手册：[branch-switch-plugin-release-guide.md](docs/operations/branch-switch-plugin-release-guide.md)
- 项目知识库：[docs/README.md](docs/README.md)

## 当前限制

- 当前版本只处理 IDEA 当前窗口中已打开或已附加的 Git 仓库。
- 当前版本不会自动创建缺失分支，也不会自动恢复 Shelf 中的变更。
- 当前版本默认只把已跟踪变更纳入保护；纯未跟踪文件通常不会阻止流程，但如果 checkout 会覆盖未跟踪文件，Git 仍可能拒绝切换并在结果中显示失败。
- `verifyPlugin` 仍需要持续纳入发布门禁；如果本地或 CI 遇到 IntelliJ Plugin Verifier 依赖解析问题，请先参考发布手册记录排查。

## 许可证

本项目基于 **MIT License** 开源，详见 [LICENSE](LICENSE)。
