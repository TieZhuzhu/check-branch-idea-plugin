# 任务：IDEA 多项目分支快速切换

**输入**：来自 `specs/001-branch-switch-plugin/` 的设计文档

**前置条件**：plan.md、spec.md、research.md、data-model.md、contracts/tool-window-ui.md、quickstart.md

**测试**：本功能涉及 Git 仓库状态、未提交变更保护和批量切换结果，任务中包含单元测试、集成测试和插件验证任务。

**文档**：所有行为、界面文案、发布流程和实现细节变更必须同步到 `docs/` 与当前 feature 文档。

**组织方式**：任务按用户故事分组，确保每个故事可以独立实施和独立验证。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行执行，不修改同一文件且不依赖未完成任务
- **[Story]**：任务所属用户故事，例如 US1、US2、US3
- 每个任务都包含明确文件路径

## 阶段 1：准备（项目骨架）

**目的**：建立 Java 21 IDEA 插件项目基础、发布流水线和中文资源入口。

- [ ] T001 创建 Gradle Kotlin DSL 项目骨架：settings.gradle.kts、build.gradle.kts、gradle.properties
- [ ] T002 配置 Gradle Wrapper 使用项目约定版本：gradle/wrapper/gradle-wrapper.properties
- [ ] T003 创建插件描述文件并声明 Java、Git、平台依赖：src/main/resources/META-INF/plugin.xml
- [ ] T004 [P] 创建中文资源文件：src/main/resources/messages/CheckBranchBundle.properties
- [ ] T005 [P] 创建源码包目录和测试包目录：src/main/java/com/augustlee/tool/checkbranch/、src/test/java/com/augustlee/tool/checkbranch/
- [ ] T006 [P] 创建插件图标占位资源：src/main/resources/META-INF/pluginIcon.svg
- [ ] T007 创建持续集成流水线：.github/workflows/ci.yml
- [ ] T008 创建标签发布流水线：.github/workflows/release.yml
- [ ] T009 更新项目构建说明：README.md
- [ ] T010 更新知识库功能文档的实施入口：docs/features/branch-switch-plugin.md

---

## 阶段 2：基础能力（阻塞前置）

**目的**：完成所有用户故事共享的数据模型、状态、通知和工具窗口基础能力。

**关键提醒**：本阶段完成前，不得开始任何用户故事实施。

- [ ] T011 [P] 创建工作区仓库模型：src/main/java/com/augustlee/tool/checkbranch/model/WorkspaceRepository.java
- [ ] T012 [P] 创建分支切换请求模型：src/main/java/com/augustlee/tool/checkbranch/model/BranchSwitchRequest.java
- [ ] T013 [P] 创建临时变更记录模型：src/main/java/com/augustlee/tool/checkbranch/model/TemporaryChangeRecord.java
- [ ] T014 [P] 创建切换结果模型：src/main/java/com/augustlee/tool/checkbranch/model/SwitchResult.java
- [ ] T015 [P] 创建结果状态枚举：src/main/java/com/augustlee/tool/checkbranch/model/SwitchResultStatus.java
- [ ] T016 [P] 创建变更保护模式枚举：src/main/java/com/augustlee/tool/checkbranch/model/ChangeProtectionMode.java
- [ ] T017 [P] 创建分支偏好状态服务：src/main/java/com/augustlee/tool/checkbranch/service/BranchPreferenceService.java
- [ ] T018 [P] 创建中文通知服务：src/main/java/com/augustlee/tool/checkbranch/notification/CheckBranchNotifier.java
- [ ] T019 创建右侧工具窗口工厂：src/main/java/com/augustlee/tool/checkbranch/CheckBranchToolWindowFactory.java
- [ ] T020 创建工具窗口主面板骨架：src/main/java/com/augustlee/tool/checkbranch/ui/BranchSwitchToolWindowContent.java
- [ ] T021 [P] 创建仓库表格模型骨架：src/main/java/com/augustlee/tool/checkbranch/ui/RepositoryTableModel.java
- [ ] T022 [P] 创建结果展示面板骨架：src/main/java/com/augustlee/tool/checkbranch/ui/SwitchResultPanel.java
- [ ] T023 [P] 创建模型校验单元测试：src/test/java/com/augustlee/tool/checkbranch/model/BranchSwitchModelTest.java
- [ ] T024 [P] 创建中文资源完整性测试：src/test/java/com/augustlee/tool/checkbranch/CheckBranchBundleTest.java

**检查点**：插件可被 IDEA 识别，工具窗口可加载空面板，模型和中文资源测试通过。

---

## 阶段 3：用户故事 1 - 扫描工作区项目分支状态（优先级：P1）

**目标**：用户打开工具窗口后能看到当前 IDEA 工作区中的 Git 仓库、当前分支、可切换状态和变更状态。

**独立测试**：打开包含多个 Git 仓库的工作区，刷新后每个可识别仓库都显示当前分支、可切换状态和变更状态，不可识别仓库显示中文原因。

### 用户故事 1 的测试

- [ ] T025 [P] [US1] 创建仓库发现服务单元测试：src/test/java/com/augustlee/tool/checkbranch/service/RepositoryDiscoveryServiceTest.java
- [ ] T026 [P] [US1] 创建仓库表格模型测试：src/test/java/com/augustlee/tool/checkbranch/ui/RepositoryTableModelTest.java
- [ ] T027 [US1] 创建多 Git 仓库刷新集成测试：src/test/java/com/augustlee/tool/checkbranch/integration/RepositoryRefreshIntegrationTest.java

### 用户故事 1 的实现

- [ ] T028 [US1] 实现仓库发现服务：src/main/java/com/augustlee/tool/checkbranch/service/RepositoryDiscoveryService.java
- [ ] T029 [US1] 在仓库发现服务中读取当前分支、阻塞状态和未提交变更状态：src/main/java/com/augustlee/tool/checkbranch/service/RepositoryDiscoveryService.java
- [ ] T030 [US1] 将仓库刷新接入工具窗口顶部刷新按钮：src/main/java/com/augustlee/tool/checkbranch/ui/BranchSwitchToolWindowContent.java
- [ ] T031 [US1] 在仓库表格中展示名称、路径、当前分支、变更状态和可切换状态：src/main/java/com/augustlee/tool/checkbranch/ui/RepositoryTableModel.java
- [ ] T032 [US1] 在工具窗口中实现空状态和刷新失败中文提示：src/main/java/com/augustlee/tool/checkbranch/ui/BranchSwitchToolWindowContent.java
- [ ] T033 [US1] 更新用户故事 1 相关文档：docs/features/branch-switch-plugin.md

**检查点**：用户故事 1 可独立演示，未实现切换也能完成仓库状态扫描和展示。

---

## 阶段 4：用户故事 2 - 批量切换到指定分支（优先级：P1）

**目标**：用户输入目标分支后，可以对选中仓库批量切换；目标分支缺失时回退到主分支。

**独立测试**：准备目标分支存在、目标分支缺失但主分支存在、目标分支和主分支均不可用的仓库，执行批量切换并验证每个仓库结果。

### 用户故事 2 的测试

- [ ] T034 [P] [US2] 创建分支切换服务单元测试：src/test/java/com/augustlee/tool/checkbranch/service/BranchSwitchServiceTest.java
- [ ] T035 [P] [US2] 创建主分支回退规则测试：src/test/java/com/augustlee/tool/checkbranch/service/MainBranchFallbackTest.java
- [ ] T036 [US2] 创建批量切换集成测试：src/test/java/com/augustlee/tool/checkbranch/integration/BatchBranchSwitchIntegrationTest.java

### 用户故事 2 的实现

- [ ] T037 [US2] 实现分支切换服务：src/main/java/com/augustlee/tool/checkbranch/service/BranchSwitchService.java
- [ ] T038 [US2] 实现目标分支本地和远端解析逻辑：src/main/java/com/augustlee/tool/checkbranch/service/BranchSwitchService.java
- [ ] T039 [US2] 实现主分支候选读取与回退规则：src/main/java/com/augustlee/tool/checkbranch/service/BranchPreferenceService.java
- [ ] T040 [US2] 实现目标分支输入校验和中文错误提示：src/main/java/com/augustlee/tool/checkbranch/ui/BranchSwitchToolWindowContent.java
- [ ] T041 [US2] 将批量切换动作接入工具窗口执行按钮：src/main/java/com/augustlee/tool/checkbranch/ui/BranchSwitchToolWindowContent.java
- [ ] T042 [US2] 将成功、回退、跳过和失败结果写入结果面板：src/main/java/com/augustlee/tool/checkbranch/ui/SwitchResultPanel.java
- [ ] T043 [US2] 更新批量切换和主分支回退文档：docs/features/branch-switch-plugin.md

**检查点**：用户故事 2 可独立演示，用户可批量切换并看到逐仓库结果。

---

## 阶段 5：用户故事 3 - 切换前保护未提交变更（优先级：P1）

**目标**：仓库存在未提交变更时，用户可选择搁置变更、降级 Git 暂存栈、跳过仓库或取消操作。

**独立测试**：准备有未提交变更的仓库，验证临时保存成功、搁置失败降级、跳过和取消路径。

### 用户故事 3 的测试

- [ ] T044 [P] [US3] 创建变更保护服务单元测试：src/test/java/com/augustlee/tool/checkbranch/service/ChangeProtectionServiceTest.java
- [ ] T045 [P] [US3] 创建临时变更记录测试：src/test/java/com/augustlee/tool/checkbranch/model/TemporaryChangeRecordTest.java
- [ ] T046 [US3] 创建未提交变更保护集成测试：src/test/java/com/augustlee/tool/checkbranch/integration/ChangeProtectionIntegrationTest.java

### 用户故事 3 的实现

- [ ] T047 [US3] 实现变更保护服务：src/main/java/com/augustlee/tool/checkbranch/service/ChangeProtectionService.java
- [ ] T048 [US3] 实现 IDEA 搁置变更优先保存路径：src/main/java/com/augustlee/tool/checkbranch/service/ChangeProtectionService.java
- [ ] T049 [US3] 实现 Git 暂存栈降级保存路径：src/main/java/com/augustlee/tool/checkbranch/service/ChangeProtectionService.java
- [ ] T050 [US3] 实现未提交变更选择提示：src/main/java/com/augustlee/tool/checkbranch/ui/BranchSwitchToolWindowContent.java
- [ ] T051 [US3] 将临时变更记录关联到切换结果：src/main/java/com/augustlee/tool/checkbranch/service/BranchSwitchService.java
- [ ] T052 [US3] 在结果面板展示手动恢复中文提示：src/main/java/com/augustlee/tool/checkbranch/ui/SwitchResultPanel.java
- [ ] T053 [US3] 更新暂存降级策略文档：docs/features/branch-switch-plugin.md

**检查点**：用户故事 3 可独立演示，未提交变更不会丢失，切换后提示用户手动恢复。

---

## 阶段 6：用户故事 4 - 单个项目精确切换（优先级：P2）

**目标**：用户可以只选择一个仓库进行目标分支切换，其他仓库不受影响。

**独立测试**：选择单个仓库执行切换，验证未选中仓库状态保持不变，结果只包含该仓库。

### 用户故事 4 的测试

- [ ] T054 [P] [US4] 创建单仓库选择测试：src/test/java/com/augustlee/tool/checkbranch/ui/SingleRepositorySelectionTest.java
- [ ] T055 [US4] 创建单仓库切换集成测试：src/test/java/com/augustlee/tool/checkbranch/integration/SingleRepositorySwitchIntegrationTest.java

### 用户故事 4 的实现

- [ ] T056 [US4] 完善仓库选择状态维护：src/main/java/com/augustlee/tool/checkbranch/ui/RepositoryTableModel.java
- [ ] T057 [US4] 在切换请求中只包含选中仓库：src/main/java/com/augustlee/tool/checkbranch/ui/BranchSwitchToolWindowContent.java
- [ ] T058 [US4] 在结果面板过滤未参与仓库：src/main/java/com/augustlee/tool/checkbranch/ui/SwitchResultPanel.java
- [ ] T059 [US4] 更新单仓库切换文档：docs/features/branch-switch-plugin.md

**检查点**：用户故事 4 可独立演示，单仓库切换不影响未选择仓库。

---

## 阶段 7：用户故事 5 - 查看切换结果与恢复提示（优先级：P2）

**目标**：用户完成切换后能清楚看到每个仓库的最终状态、失败原因、回退信息和恢复提示。

**独立测试**：执行混合结果切换，确认成功、回退、跳过、失败和待手动恢复状态都有中文说明。

### 用户故事 5 的测试

- [ ] T060 [P] [US5] 创建结果面板展示测试：src/test/java/com/augustlee/tool/checkbranch/ui/SwitchResultPanelTest.java
- [ ] T061 [P] [US5] 创建通知服务测试：src/test/java/com/augustlee/tool/checkbranch/notification/CheckBranchNotifierTest.java
- [ ] T062 [US5] 创建混合结果集成测试：src/test/java/com/augustlee/tool/checkbranch/integration/MixedSwitchResultIntegrationTest.java

### 用户故事 5 的实现

- [ ] T063 [US5] 完善结果面板状态分组和中文文案：src/main/java/com/augustlee/tool/checkbranch/ui/SwitchResultPanel.java
- [ ] T064 [US5] 实现失败、跳过和待恢复通知：src/main/java/com/augustlee/tool/checkbranch/notification/CheckBranchNotifier.java
- [ ] T065 [US5] 在结果中展示请求分支、最终分支和回退标识：src/main/java/com/augustlee/tool/checkbranch/ui/SwitchResultPanel.java
- [ ] T066 [US5] 实现当前项目会话内最近一次切换结果保留策略：src/main/java/com/augustlee/tool/checkbranch/service/SwitchResultStateService.java
- [ ] T067 [US5] 更新结果反馈和恢复提示文档：docs/features/branch-switch-plugin.md

**检查点**：用户故事 5 可独立演示，混合结果和恢复提示清晰可追踪。

---

## 阶段 8：收尾与横切事项

**目的**：完成插件验证、发布流水线、文档同步和宪章合规检查。

- [ ] T068 [P] 补齐工具窗口界面契约实现记录：specs/001-branch-switch-plugin/contracts/tool-window-ui.md
- [ ] T069 [P] 更新快速开始验证步骤：specs/001-branch-switch-plugin/quickstart.md
- [ ] T070 [P] 更新技术选型文档中的实际构建命令：docs/decisions/idea-plugin-tech-stack.md
- [ ] T071 执行单元测试并修复问题：build.gradle.kts
- [ ] T072 执行插件构建并修复问题：build.gradle.kts
- [ ] T073 执行插件验证并修复问题：build.gradle.kts
- [ ] T074 执行快速开始手动验证并记录结果：specs/001-branch-switch-plugin/quickstart.md
- [ ] T075 检查所有新增或修改文件为 UTF-8（非 BOM）：specs/001-branch-switch-plugin/quickstart.md
- [ ] T076 检查代码注释、JavaDoc、日志、通知和用户可见输入输出均为中文：src/main/java/com/augustlee/tool/checkbranch/
- [ ] T077 确认所有 JavaDoc 作者署名为 August Lee：src/main/java/com/augustlee/tool/checkbranch/

---

## 依赖与执行顺序

### 阶段依赖

- **阶段 1 准备**：无依赖，可立即开始
- **阶段 2 基础能力**：依赖阶段 1，阻塞所有用户故事
- **阶段 3 用户故事 1**：依赖阶段 2，是最小可用版本第一步
- **阶段 4 用户故事 2**：依赖阶段 3 的仓库状态能力
- **阶段 5 用户故事 3**：依赖阶段 4 的切换服务
- **阶段 6 用户故事 4**：依赖阶段 4 的切换服务和阶段 2 的选择模型
- **阶段 7 用户故事 5**：依赖阶段 4 和阶段 5 的结果与临时变更记录
- **阶段 8 收尾**：依赖目标用户故事完成

### 用户故事依赖

- **US1 扫描工作区项目分支状态**：基础能力完成后可独立交付，是 MVP 必需范围
- **US2 批量切换到指定分支**：依赖 US1 的仓库状态，完成后形成核心批量切换 MVP
- **US3 切换前保护未提交变更**：依赖 US2 的切换流程，完成后达到安全可用
- **US4 单个项目精确切换**：依赖选择模型和切换服务，可在 US2 后独立推进
- **US5 查看切换结果与恢复提示**：依赖切换结果和临时变更记录，可在 US3 后完善

### 单个用户故事内部顺序

- 测试任务先于实现任务
- 模型先于服务
- 服务先于界面接入
- 界面接入先于文档更新
- 文档更新与对应行为变更在同一阶段完成

---

## 并行机会

- 阶段 1 的 T004、T005、T006 可并行。
- 阶段 2 的模型、枚举、通知和测试骨架可并行。
- US1 中仓库发现测试和表格模型测试可并行。
- US2 中分支切换服务测试和主分支回退规则测试可并行。
- US3 中变更保护服务测试和临时变更记录测试可并行。
- US5 中结果面板测试和通知服务测试可并行。
- 阶段 8 的文档更新任务可并行。

## 并行示例

```text
任务："T011 创建工作区仓库模型：src/main/java/com/augustlee/tool/checkbranch/model/WorkspaceRepository.java"
任务："T012 创建分支切换请求模型：src/main/java/com/augustlee/tool/checkbranch/model/BranchSwitchRequest.java"
任务："T013 创建临时变更记录模型：src/main/java/com/augustlee/tool/checkbranch/model/TemporaryChangeRecord.java"
任务："T014 创建切换结果模型：src/main/java/com/augustlee/tool/checkbranch/model/SwitchResult.java"
```

```text
任务："T034 创建分支切换服务单元测试：src/test/java/com/augustlee/tool/checkbranch/service/BranchSwitchServiceTest.java"
任务："T035 创建主分支回退规则测试：src/test/java/com/augustlee/tool/checkbranch/service/MainBranchFallbackTest.java"
```

## 实施策略

### 最小可用版本优先

1. 完成阶段 1 和阶段 2，确保插件可加载。
2. 完成 US1，先交付仓库发现和状态展示。
3. 完成 US2，交付批量分支切换核心能力。
4. 完成 US3，补齐未提交变更保护，达到安全可用。

### 增量交付

1. US1：可查看多仓库状态。
2. US2：可批量切换分支。
3. US3：可保护未提交变更。
4. US4：可单仓库精确切换。
5. US5：可清楚追踪混合结果和恢复提示。

### 发布前门禁

- 通过单元测试和集成测试。
- 通过插件构建和插件验证。
- 快速开始手动验证完成。
- 所有文档、注释、日志、通知和用户可见输入输出均为中文。
- 所有新增或修改文件为 UTF-8（非 BOM）。

## 任务统计

- 总任务数：77
- 准备任务：10
- 基础任务：14
- US1：9
- US2：10
- US3：10
- US4：6
- US5：8
- 收尾任务：10
