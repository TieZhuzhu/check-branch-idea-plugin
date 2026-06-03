package com.augustlee.tool.checkbranch.model;

import java.time.Instant;

/**
 * 表示切换分支前临时保存未提交变更的记录。
 *
 * @author August Lee
 */
public class TemporaryChangeRecord {

    private final String recordId;
    private final String repositoryId;
    private final ChangeProtectionMode method;
    private final String label;
    private final Instant createdAt;
    private final String restoreHint;
    private final boolean requiresManualRestore;

    /**
     * 创建临时变更记录。
     *
     * @param recordId 记录唯一标识
     * @param repositoryId 仓库唯一标识
     * @param method 保存方式
     * @param label 面向用户的记录名称
     * @param createdAt 创建时间
     * @param restoreHint 恢复提示
     * @param requiresManualRestore 是否需要手动恢复
     */
    public TemporaryChangeRecord(
            String recordId,
            String repositoryId,
            ChangeProtectionMode method,
            String label,
            Instant createdAt,
            String restoreHint,
            boolean requiresManualRestore
    ) {
        this.recordId = requireText(recordId, "临时变更记录标识不能为空");
        this.repositoryId = requireText(repositoryId, "仓库标识不能为空");
        this.method = method == null ? ChangeProtectionMode.SHELVE : method;
        this.label = requireText(label, "临时变更记录名称不能为空");
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.restoreHint = requireText(restoreHint, "恢复提示不能为空");
        this.requiresManualRestore = requiresManualRestore;
        validate();
    }

    /**
     * 校验当前记录是否合法。
     *
     * @throws IllegalArgumentException 当关键字段非法时抛出
     */
    public void validate() {
        requireText(recordId, "临时变更记录标识不能为空");
        requireText(repositoryId, "仓库标识不能为空");
        requireText(label, "临时变更记录名称不能为空");
        requireText(restoreHint, "恢复提示不能为空");
        if (method == ChangeProtectionMode.ASK || method == ChangeProtectionMode.SKIP) {
            throw new IllegalArgumentException("临时变更记录的保存方式必须是实际保存方式");
        }
    }

    /**
     * 返回记录唯一标识。
     *
     * @return 记录唯一标识
     */
    public String getRecordId() {
        return recordId;
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
     * 返回保存方式。
     *
     * @return 保存方式
     */
    public ChangeProtectionMode getMethod() {
        return method;
    }

    /**
     * 返回记录展示名称。
     *
     * @return 记录展示名称
     */
    public String getLabel() {
        return label;
    }

    /**
     * 返回创建时间。
     *
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 返回恢复提示。
     *
     * @return 恢复提示
     */
    public String getRestoreHint() {
        return restoreHint;
    }

    /**
     * 返回是否需要用户手动恢复。
     *
     * @return 是否需要用户手动恢复
     */
    public boolean isRequiresManualRestore() {
        return requiresManualRestore;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
