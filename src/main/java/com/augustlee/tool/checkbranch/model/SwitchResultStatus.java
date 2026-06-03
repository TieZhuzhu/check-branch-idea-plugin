package com.augustlee.tool.checkbranch.model;

/**
 * 定义单个仓库分支切换结果的状态枚举。
 *
 * @author August Lee
 */
public enum SwitchResultStatus {

    /**
     * 表示仓库成功切换到目标分支。
     */
    SUCCESS("成功"),

    /**
     * 表示仓库未找到目标分支，已回退到主分支。
     */
    FALLBACK_TO_MAIN("主分支回退"),

    /**
     * 表示仓库本次被跳过，没有执行切换。
     */
    SKIPPED("跳过"),

    /**
     * 表示仓库切换失败。
     */
    FAILED("失败"),

    /**
     * 表示仓库切换完成，但仍需用户手动恢复变更。
     */
    MANUAL_RESTORE_REQUIRED("待手动恢复");

    private final String displayName;

    SwitchResultStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 返回面向用户展示的中文状态名称。
     *
     * @return 中文状态名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
