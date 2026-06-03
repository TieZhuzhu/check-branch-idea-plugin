package com.augustlee.tool.checkbranch.model;

import java.util.Objects;

/**
 * 表示当前 IDEA 工作区中检测到的一个 Git 仓库。
 *
 * @author August Lee
 */
public class WorkspaceRepository {

    private final String id;
    private final String rootPath;
    private String displayName;
    private String currentBranch;
    private String targetBranchState;
    private String mainBranchCandidate;
    private boolean hasUncommittedChanges;
    private boolean operationBlocked;
    private String blockReason;
    private boolean selected;
    private String lastOperationResultSummary;

    /**
     * 创建工作区仓库模型。
     *
     * @param id 仓库唯一标识
     * @param displayName 仓库展示名称
     * @param rootPath 仓库根路径
     */
    public WorkspaceRepository(String id, String displayName, String rootPath) {
        this.id = requireText(id, "仓库唯一标识不能为空");
        this.rootPath = requireText(rootPath, "仓库根路径不能为空");
        this.displayName = hasText(displayName) ? displayName.trim() : deriveDisplayName(rootPath);
        this.currentBranch = "";
        this.targetBranchState = "未知";
        this.mainBranchCandidate = "";
        this.blockReason = "";
        this.lastOperationResultSummary = "";
    }

    /**
     * 校验当前仓库模型是否合法。
     *
     * @throws IllegalArgumentException 当核心字段为空时抛出
     */
    public void validate() {
        requireText(id, "仓库唯一标识不能为空");
        requireText(rootPath, "仓库根路径不能为空");
        if (!hasText(displayName)) {
            displayName = deriveDisplayName(rootPath);
        }
    }

    /**
     * 返回仓库唯一标识。
     *
     * @return 仓库唯一标识
     */
    public String getId() {
        return id;
    }

    /**
     * 返回仓库展示名称。
     *
     * @return 仓库展示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置仓库展示名称。
     *
     * @param displayName 仓库展示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = hasText(displayName) ? displayName.trim() : deriveDisplayName(rootPath);
    }

    /**
     * 返回仓库根路径。
     *
     * @return 仓库根路径
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * 返回当前分支名称。
     *
     * @return 当前分支名称
     */
    public String getCurrentBranch() {
        return currentBranch;
    }

    /**
     * 设置当前分支名称。
     *
     * @param currentBranch 当前分支名称
     */
    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = normalize(currentBranch);
    }

    /**
     * 返回目标分支状态说明。
     *
     * @return 目标分支状态说明
     */
    public String getTargetBranchState() {
        return targetBranchState;
    }

    /**
     * 设置目标分支状态说明。
     *
     * @param targetBranchState 目标分支状态说明
     */
    public void setTargetBranchState(String targetBranchState) {
        this.targetBranchState = normalize(targetBranchState);
    }

    /**
     * 返回主分支候选名称。
     *
     * @return 主分支候选名称
     */
    public String getMainBranchCandidate() {
        return mainBranchCandidate;
    }

    /**
     * 设置主分支候选名称。
     *
     * @param mainBranchCandidate 主分支候选名称
     */
    public void setMainBranchCandidate(String mainBranchCandidate) {
        this.mainBranchCandidate = normalize(mainBranchCandidate);
    }

    /**
     * 返回仓库是否存在未提交变更。
     *
     * @return 是否存在未提交变更
     */
    public boolean hasUncommittedChanges() {
        return hasUncommittedChanges;
    }

    /**
     * 设置仓库是否存在未提交变更。
     *
     * @param hasUncommittedChanges 是否存在未提交变更
     */
    public void setHasUncommittedChanges(boolean hasUncommittedChanges) {
        this.hasUncommittedChanges = hasUncommittedChanges;
    }

    /**
     * 返回当前仓库是否被阻塞，无法执行切换。
     *
     * @return 是否被阻塞
     */
    public boolean isOperationBlocked() {
        return operationBlocked;
    }

    /**
     * 设置当前仓库是否被阻塞。
     *
     * @param operationBlocked 是否被阻塞
     */
    public void setOperationBlocked(boolean operationBlocked) {
        this.operationBlocked = operationBlocked;
    }

    /**
     * 返回阻塞原因说明。
     *
     * @return 阻塞原因说明
     */
    public String getBlockReason() {
        return blockReason;
    }

    /**
     * 设置阻塞原因说明。
     *
     * @param blockReason 阻塞原因说明
     */
    public void setBlockReason(String blockReason) {
        this.blockReason = normalize(blockReason);
    }

    /**
     * 返回用户是否选择当前仓库参与切换。
     *
     * @return 是否已选择
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * 设置用户是否选择当前仓库参与切换。
     *
     * @param selected 是否已选择
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * 返回最近一次操作结果摘要。
     *
     * @return 最近一次操作结果摘要
     */
    public String getLastOperationResultSummary() {
        return lastOperationResultSummary;
    }

    /**
     * 设置最近一次操作结果摘要。
     *
     * @param lastOperationResultSummary 最近一次操作结果摘要
     */
    public void setLastOperationResultSummary(String lastOperationResultSummary) {
        this.lastOperationResultSummary = normalize(lastOperationResultSummary);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String deriveDisplayName(String path) {
        String normalizedPath = Objects.requireNonNull(path, "路径不能为空").replace('\\', '/');
        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalizedPath.substring(lastSlashIndex + 1) : normalizedPath;
    }
}
