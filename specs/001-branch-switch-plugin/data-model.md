# 数据模型：IDEA 多项目分支快速切换

## 工作区仓库

**用途**：表示当前 IDEA 工作区中检测到的一个 Git 仓库。

**字段**：

- `id`：仓库唯一标识，建议使用规范化根路径。
- `displayName`：中文界面中展示的仓库名称。
- `rootPath`：仓库根路径。
- `currentBranch`：当前分支名称；读取失败时为空。
- `targetBranchState`：目标分支状态，包括本地存在、远端存在、缺失、未知。
- `mainBranchCandidate`：可回退的主分支候选，例如 `main` 或 `master`。
- `hasUncommittedChanges`：是否存在未提交变更。
- `operationBlocked`：是否因合并、变基、拣选提交或冲突而阻塞操作。
- `blockReason`：不可切换原因，必须可转为中文提示。
- `selected`：用户是否选择该仓库参与本次切换。

**校验规则**：

- `rootPath` 必须非空且指向可访问目录。
- `displayName` 为空时使用目录名兜底。
- `operationBlocked` 为真时，该仓库不得进入切换执行队列。

**状态变化**：

```text
未扫描 -> 已发现 -> 状态已刷新 -> 待切换 -> 已切换/已跳过/失败
```

## 分支切换请求

**用途**：表示用户发起的一次分支切换操作。

**字段**：

- `requestId`：操作唯一标识。
- `targetBranch`：用户输入的目标分支。
- `repositoryIds`：参与切换的仓库标识列表。
- `changeProtectionMode`：变更保护模式，包括询问、搁置变更、Git 暂存栈、跳过。
- `startedAt`：操作开始时间。
- `cancelOnFirstFailure`：首版固定为假，表示允许部分成功。

**校验规则**：

- `targetBranch` 去除首尾空白后必须非空。
- `repositoryIds` 至少包含一个可切换仓库。
- 目标分支不得包含明显非法字符或空白输入。

## 临时变更记录

**用途**：记录切换前临时保存的用户变更。

**字段**：

- `recordId`：记录唯一标识。
- `repositoryId`：所属工作区仓库。
- `method`：保存方式，包括搁置变更、Git 暂存栈。
- `label`：面向用户展示的记录名称。
- `createdAt`：保存时间。
- `restoreHint`：中文恢复提示。
- `requiresManualRestore`：首版固定为真。

**校验规则**：

- `method` 必须明确，不能使用未知值。
- `restoreHint` 必须非空。
- 记录创建成功后，即使后续切换失败也必须保留。

## 切换结果

**用途**：表示单个仓库在一次切换请求中的最终结果。

**字段**：

- `repositoryId`：所属工作区仓库。
- `status`：结果状态，包括成功、主分支回退、跳过、失败、待手动恢复。
- `requestedBranch`：用户输入的目标分支。
- `finalBranch`：操作结束后的分支。
- `fallbackUsed`：是否使用主分支回退。
- `temporaryChangeRecordId`：相关临时变更记录，可为空。
- `message`：中文结果说明。
- `failureReason`：中文失败原因，可为空。
- `finishedAt`：操作结束时间。

**校验规则**：

- `status` 为失败时，`failureReason` 必须非空。
- `fallbackUsed` 为真时，`finalBranch` 必须为识别出的主分支。
- 存在临时变更记录时，结果必须提示用户手动恢复。

## 切换结果状态

**用途**：保存当前项目会话内最近一次切换结果，确保关闭并重新打开工具窗口后仍能查看结果摘要。

**字段**：

- `lastResults`：最近一次逐仓库切换结果列表。
- `updatedAt`：结果更新时间。
- `projectSessionOnly`：是否仅当前项目会话保留，首版固定为真。

**校验规则**：

- `lastResults` 为空时，结果区展示中文空状态。
- 工具窗口关闭后重新打开时，必须读取并展示最近一次结果摘要。
- 关闭 IDEA 项目后不要求保留 `lastResults`。

## 分支偏好

**用途**：保存项目级偏好，减少重复输入。

**字段**：

- `recentTargetBranches`：最近使用过的目标分支列表。
- `mainBranchNames`：主分支候选顺序，默认 `main`、`master`。
- `defaultChangeProtectionMode`：默认变更保护策略。
- `lastRefreshAt`：最近刷新时间。

**校验规则**：

- `recentTargetBranches` 去重后按最近使用排序。
- `mainBranchNames` 至少包含一个非空名称。
- 所有用户可见偏好名称必须能展示为中文。
