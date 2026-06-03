# 实施计划：IDEA 多项目分支快速切换

**分支**：`001-branch-switch-plugin` | **日期**：2026-06-03 | **规格**：[spec.md](spec.md)

**输入**：来自 `specs/001-branch-switch-plugin/spec.md` 的功能规格

**说明**：本计划覆盖技术实现细节，后续 `/speckit-tasks` 将基于本计划拆解任务。

## 摘要

本功能将实现一个 IntelliJ IDEA 2024.2+ 插件，在右侧固定工具窗口中列出当前 IDEA 窗口内的 Git
仓库，支持输入目标分支后对选中仓库进行批量或单仓库切换。切换前会检测未提交变更，优先使用
IDEA 搁置变更能力，失败或不可用时降级到 Git 暂存栈；切换后只提示用户手动恢复，不自动应用
暂存内容。项目采用 Java 21、Gradle Kotlin DSL 和 IntelliJ Platform Gradle Plugin 2.x。

## 技术上下文

**语言/版本**：Java 21

**主要依赖**：IntelliJ Platform Gradle Plugin 2.x、IntelliJ IDEA Community 2024.2、平台 Java 模块、Git 插件能力

**存储**：使用 IDEA 项目级状态服务保存用户偏好，包括最近目标分支、主分支候选顺序、默认变更保护策略；使用项目会话级结果状态服务保存最近一次切换结果；不引入数据库

**测试**：Gradle 单元测试、插件验证、可运行 IDE 手动验证、Git 仓库临时目录集成测试

**目标平台**：IntelliJ IDEA Community 2024.2+，插件运行环境对齐 JBR21

**项目类型**：IntelliJ IDEA 桌面插件

**性能目标**：10 个仓库全部可切换时，用户可在 2 分钟内完成统一分支切换；刷新后至少 95% 仓库展示准确状态

**约束**：所有产出文档、代码注释、用户可见输入输出和日志提示使用中文；文件使用 UTF-8（非 BOM）；JavaDoc 必须完整且作者为 `August Lee`

**规模/范围**：初始版本只处理单个 IDEA 窗口中已打开或已附加的 Git 仓库，不扫描任意磁盘目录，不自动拉取，不自动创建分支，不自动恢复暂存变更

## 宪章检查

*门禁：必须在第 0 阶段研究前通过，并在第 1 阶段设计后复查。*

- 已明确记录假设、模糊点和更简单可行方案：通过 `spec.md`、`research.md` 和本计划记录范围边界。
- 方案保持在当前功能所需的最小范围内：只实现当前 IDEA 窗口内仓库切换，不做磁盘扫描、自动拉取或自动恢复。
- 所有产出文档、代码注释、用户可见输入输出和交付说明均计划使用中文：本计划和后续产物均按中文生成。
- 所有新增或修改文件均计划使用 UTF-8（非 BOM）编码：验证步骤纳入快速开始和任务阶段。
- `docs/` 文档更新已标明目标文件或计划新增条目：已更新功能文档和技术选型文档，后续实现需继续同步。
- 数据库写入操作：本功能不涉及数据库写入。
- Java 代码要求：所有类、方法、参数、返回值和异常补充中文 JavaDoc，作者署名使用 `August Lee`。

**门禁结果**：通过。当前计划没有未解释的宪章违背项。

## 项目结构

### 文档（当前功能）

```text
specs/001-branch-switch-plugin/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── tool-window-ui.md
├── checklists/
│   ├── requirements.md
│   └── ux.md
└── tasks.md
```

### 源代码（仓库根目录）

```text
src/
├── main/
│   ├── java/
│   │   └── com/augustlee/tool/checkbranch/
│   │       ├── CheckBranchToolWindowFactory.java
│   │       ├── ui/
│   │       │   ├── BranchSwitchToolWindowContent.java
│   │       │   ├── RepositoryTableModel.java
│   │       │   └── SwitchResultPanel.java
│   │       ├── service/
│   │       │   ├── RepositoryDiscoveryService.java
│   │       │   ├── BranchSwitchService.java
│   │       │   ├── ChangeProtectionService.java
│   │       │   ├── BranchPreferenceService.java
│   │       │   └── SwitchResultStateService.java
│   │       ├── model/
│   │       │   ├── WorkspaceRepository.java
│   │       │   ├── BranchSwitchRequest.java
│   │       │   ├── TemporaryChangeRecord.java
│   │       │   └── SwitchResult.java
│   │       └── notification/
│   │           └── CheckBranchNotifier.java
│   └── resources/
│       ├── META-INF/
│       │   └── plugin.xml
│       └── messages/
│           └── CheckBranchBundle.properties
└── test/
    └── java/
        └── com/augustlee/tool/checkbranch/
```

```text
.github/
└── workflows/
    ├── ci.yml
    └── release.yml
```

**结构决策**：采用单插件项目结构。界面、服务、模型、通知分包，避免为一次性逻辑创建额外抽象层；发布流水线独立放在 `.github/workflows/`。

## 技术设计细节

### 构建与插件配置

- 使用 Gradle Kotlin DSL。
- 使用 `org.jetbrains.intellij.platform` 2.x 插件。
- Gradle 使用 JDK 21，源码和目标字节码均为 21。
- IntelliJ Platform 依赖目标为 IDEA Community 2024.2+。
- 插件依赖声明包含平台基础能力、Java 模块和 Git 插件能力。
- `plugin.xml` 注册右侧工具窗口，工具窗口名称使用中文短名，`anchor` 设置为右侧。
- 构建任务包括 `buildPlugin`、插件验证、单元测试和发布产物上传。

### 界面实现

- `CheckBranchToolWindowFactory` 作为工具窗口入口，负责创建内容面板。
- 主界面采用 Swing/JB 组件，保持 IDEA 原生体验。
- 顶部区域包含目标分支输入、刷新按钮、批量切换按钮和默认策略入口。
- 中间区域为仓库列表，展示仓库名称、路径、当前分支、目标分支状态、变更状态和勾选状态。
- 底部区域展示本次操作结果，包括成功、主分支回退、跳过、失败和待手动恢复。
- 最近一次切换结果由项目会话级 `SwitchResultStateService` 保存，关闭工具窗口后重新打开仍展示结果摘要；关闭 IDEA 项目后不做跨会话保留。
- 所有用户可见文案使用中文资源文件统一管理，便于后续维护。

### 仓库发现

- 使用 IDEA 项目上下文发现当前窗口中已打开或已附加的 Git 仓库。
- 仓库发现只返回属于当前项目上下文的 Git 根目录。
- 不扫描任意本地磁盘目录，避免首版范围扩大和性能不可控。
- 不可识别或不可访问仓库显示为不可切换，并给出中文原因。

### 分支切换

- 刷新时获取仓库当前分支、可用本地分支、可解析远端分支和主分支候选。
- 切换时优先匹配目标分支；目标分支不存在时按配置或默认顺序回退到主分支。
- 对处于合并、变基、拣选提交或冲突状态的仓库直接跳过。
- 批量操作允许部分成功，不自动回滚已完成仓库。
- 每个仓库独立生成 `SwitchResult`，便于结果区域展示和后续测试。
- 本次切换结束后，将逐仓库 `SwitchResult` 写入 `SwitchResultStateService`，不混入分支偏好状态。

### 变更保护

- 切换前检测未提交变更。
- 用户可选择临时保存、跳过仓库或取消本次操作。
- 临时保存优先使用 IDEA 搁置变更能力。
- 搁置不可用或失败时，经用户同意后降级到 Git 暂存栈。
- 切换后不自动恢复变更，只在结果中提示恢复方式和冲突风险。

### 通知与日志

- 关键结果通过工具窗口结果区域展示。
- 需要用户注意的失败、跳过和待恢复状态通过 IDEA 通知提示。
- 日志仅记录操作摘要、仓库路径、状态和异常原因，不记录用户代码内容。
- 日志、通知、错误提示均使用中文。

## 第 0 阶段：研究输出

研究结论记录在 [research.md](research.md)，所有技术疑点均已解析，无待澄清项。

## 第 1 阶段：设计输出

- 数据模型：[data-model.md](data-model.md)
- 界面契约：[contracts/tool-window-ui.md](contracts/tool-window-ui.md)
- 快速开始：[quickstart.md](quickstart.md)
- 代理上下文：已更新 [AGENTS.md](../../AGENTS.md)

## 文档影响

- **需要更新的文档**：`docs/features/branch-switch-plugin.md`、`docs/decisions/idea-plugin-tech-stack.md`
- **知识库变更**：技术实现计划新增后，功能文档需要引用计划与快速开始；技术选型文档保持 Java 21 和 IDEA 2024.2+ 决策。
- **延期文档**：无

## 复杂度跟踪

当前无必须解释的宪章违背项。

## 设计后宪章复查

- 全部新增设计产物使用中文。
- 技术计划保持最小实现范围。
- 不涉及数据库写入。
- 已同步 `docs/` 功能文档入口。
- 后续代码阶段必须继续满足中文 JavaDoc、中文用户可见文案和 UTF-8（非 BOM）要求。

**复查结果**：通过，可进入 `/speckit-tasks`。
