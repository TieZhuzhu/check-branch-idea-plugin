package com.augustlee.tool.checkbranch.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示用户发起的一次分支切换请求。
 *
 * @author August Lee
 */
public class BranchSwitchRequest {

    private final String requestId;
    private final String targetBranch;
    private final List<String> repositoryIds;
    private final ChangeProtectionMode changeProtectionMode;
    private final Instant startedAt;
    private final boolean cancelOnFirstFailure;

    /**
     * 创建分支切换请求。
     *
     * @param requestId 请求唯一标识
     * @param targetBranch 目标分支名称
     * @param repositoryIds 参与切换的仓库标识列表
     * @param changeProtectionMode 变更保护模式
     * @param startedAt 请求发起时间
     * @param cancelOnFirstFailure 是否在首个失败后终止
     */
    public BranchSwitchRequest(
            String requestId,
            String targetBranch,
            List<String> repositoryIds,
            ChangeProtectionMode changeProtectionMode,
            Instant startedAt,
            boolean cancelOnFirstFailure
    ) {
        this.requestId = requireText(requestId, "请求唯一标识不能为空");
        this.targetBranch = requireText(targetBranch, "目标分支不能为空");
        this.repositoryIds = new ArrayList<>(repositoryIds == null ? List.of() : repositoryIds);
        this.changeProtectionMode = changeProtectionMode == null ? ChangeProtectionMode.ASK : changeProtectionMode;
        this.startedAt = startedAt == null ? Instant.now() : startedAt;
        this.cancelOnFirstFailure = cancelOnFirstFailure;
        validate();
    }

    /**
     * 校验当前请求是否合法。
     *
     * @throws IllegalArgumentException 当关键字段非法时抛出
     */
    public void validate() {
        requireText(requestId, "请求唯一标识不能为空");
        String normalizedTargetBranch = requireText(targetBranch, "目标分支不能为空");
        if (containsIllegalBranchCharacter(normalizedTargetBranch)) {
            throw new IllegalArgumentException("目标分支包含非法字符");
        }
        if (repositoryIds.isEmpty()) {
            throw new IllegalArgumentException("至少需要选择一个仓库");
        }
    }

    /**
     * 返回请求唯一标识。
     *
     * @return 请求唯一标识
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 返回目标分支名称。
     *
     * @return 目标分支名称
     */
    public String getTargetBranch() {
        return targetBranch;
    }

    /**
     * 返回参与切换的仓库标识列表。
     *
     * @return 不可变仓库标识列表
     */
    public List<String> getRepositoryIds() {
        return Collections.unmodifiableList(repositoryIds);
    }

    /**
     * 返回变更保护模式。
     *
     * @return 变更保护模式
     */
    public ChangeProtectionMode getChangeProtectionMode() {
        return changeProtectionMode;
    }

    /**
     * 返回请求开始时间。
     *
     * @return 请求开始时间
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * 返回是否在首个失败后终止。
     *
     * @return 是否在首个失败后终止
     */
    public boolean isCancelOnFirstFailure() {
        return cancelOnFirstFailure;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean containsIllegalBranchCharacter(String branchName) {
        return branchName.contains(" ")
                || branchName.contains("~")
                || branchName.contains("^")
                || branchName.contains(":")
                || branchName.contains("?")
                || branchName.contains("*")
                || branchName.contains("[")
                || branchName.contains("\\");
    }
}
