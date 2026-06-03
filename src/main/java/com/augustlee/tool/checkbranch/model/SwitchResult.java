package com.augustlee.tool.checkbranch.model;

import java.time.Instant;

/**
 * 表示单个仓库在一次分支切换请求中的最终结果。
 *
 * @author August Lee
 */
public class SwitchResult {

    private final String repositoryId;
    private final SwitchResultStatus status;
    private final String requestedBranch;
    private final String finalBranch;
    private final boolean fallbackUsed;
    private final String temporaryChangeRecordId;
    private final String message;
    private final String failureReason;
    private final Instant finishedAt;
    private final String branchSource;

    /**
     * 创建分支切换结果。
     *
     * @param repositoryId 所属仓库标识
     * @param status 结果状态
     * @param requestedBranch 请求分支
     * @param finalBranch 最终分支
     * @param fallbackUsed 是否使用主分支回退
     * @param temporaryChangeRecordId 临时变更记录标识
     * @param message 结果提示信息
     * @param failureReason 失败原因
     * @param finishedAt 结果完成时间
     * @param branchSource 目标分支来源
     */
    public SwitchResult(
            String repositoryId,
            SwitchResultStatus status,
            String requestedBranch,
            String finalBranch,
            boolean fallbackUsed,
            String temporaryChangeRecordId,
            String message,
            String failureReason,
            Instant finishedAt,
            String branchSource
    ) {
        this.repositoryId = requireText(repositoryId, "仓库标识不能为空");
        this.status = status == null ? SwitchResultStatus.SUCCESS : status;
        this.requestedBranch = requireText(requestedBranch, "请求分支不能为空");
        this.finalBranch = finalBranch == null ? "" : finalBranch.trim();
        this.fallbackUsed = fallbackUsed;
        this.temporaryChangeRecordId = temporaryChangeRecordId == null ? "" : temporaryChangeRecordId.trim();
        this.message = message == null ? "" : message.trim();
        this.failureReason = failureReason == null ? "" : failureReason.trim();
        this.finishedAt = finishedAt == null ? Instant.now() : finishedAt;
        this.branchSource = branchSource == null ? "" : branchSource.trim();
        validate();
    }

    /**
     * 校验当前结果是否合法。
     *
     * @throws IllegalArgumentException 当结果字段不满足约束时抛出
     */
    public void validate() {
        requireText(repositoryId, "仓库标识不能为空");
        requireText(requestedBranch, "请求分支不能为空");
        if (status == SwitchResultStatus.FAILED && failureReason.isEmpty()) {
            throw new IllegalArgumentException("失败结果必须提供失败原因");
        }
        if (fallbackUsed && finalBranch.isEmpty()) {
            throw new IllegalArgumentException("主分支回退结果必须提供最终分支");
        }
    }

    /**
     * 返回所属仓库标识。
     *
     * @return 所属仓库标识
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * 返回结果状态。
     *
     * @return 结果状态
     */
    public SwitchResultStatus getStatus() {
        return status;
    }

    /**
     * 返回请求分支名称。
     *
     * @return 请求分支名称
     */
    public String getRequestedBranch() {
        return requestedBranch;
    }

    /**
     * 返回最终分支名称。
     *
     * @return 最终分支名称
     */
    public String getFinalBranch() {
        return finalBranch;
    }

    /**
     * 返回是否使用主分支回退。
     *
     * @return 是否使用主分支回退
     */
    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    /**
     * 返回关联的临时变更记录标识。
     *
     * @return 临时变更记录标识
     */
    public String getTemporaryChangeRecordId() {
        return temporaryChangeRecordId;
    }

    /**
     * 返回结果提示信息。
     *
     * @return 结果提示信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 返回失败原因。
     *
     * @return 失败原因
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 返回完成时间。
     *
     * @return 完成时间
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }

    /**
     * 返回目标分支来源说明。
     *
     * @return 目标分支来源说明
     */
    public String getBranchSource() {
        return branchSource;
    }

    /**
     * 返回适合列表展示的中文摘要。
     *
     * @return 中文摘要
     */
    public String toSummaryText() {
        StringBuilder summaryBuilder = new StringBuilder(status.getDisplayName())
                .append("：")
                .append(requestedBranch)
                .append(" -> ")
                .append(finalBranch.isEmpty() ? "未切换" : finalBranch);

        if (!branchSource.isEmpty()) {
            summaryBuilder.append("（").append(branchSource).append("）");
        }
        if (!message.isEmpty()) {
            summaryBuilder.append(" - ").append(message);
        }
        return summaryBuilder.toString();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
