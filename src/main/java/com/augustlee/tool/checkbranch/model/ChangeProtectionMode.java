package com.augustlee.tool.checkbranch.model;

/**
 * 定义切换前处理未提交变更的模式。
 *
 * @author August Lee
 */
public enum ChangeProtectionMode {

    /**
     * 表示执行切换前先询问用户如何处理未提交变更。
     */
    ASK("询问"),

    /**
     * 表示优先使用 IDEA 搁置变更保存未提交内容。
     */
    SHELVE("搁置变更"),

    /**
     * 表示使用 Git 暂存栈保存未提交内容。
     */
    GIT_STASH("Git 暂存栈"),

    /**
     * 表示遇到未提交变更时直接跳过当前仓库。
     */
    SKIP("跳过仓库");

    private final String displayName;

    ChangeProtectionMode(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 返回面向用户展示的中文模式名称。
     *
     * @return 中文模式名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
