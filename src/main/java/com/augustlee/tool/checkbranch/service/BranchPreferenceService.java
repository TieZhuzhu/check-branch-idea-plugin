package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 保存当前项目与分支切换偏好相关的状态数据。
 *
 * @author August Lee
 */
@Service(Service.Level.PROJECT)
@State(
        name = "CheckBranchBranchPreferenceService",
        storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public final class BranchPreferenceService implements PersistentStateComponent<BranchPreferenceService.StateBean> {

    private static final Pattern INVALID_BRANCH_PATTERN = Pattern.compile("[ ~^:?*\\\\\\[]");
    private StateBean state = new StateBean();

    /**
     * 返回项目级分支偏好服务实例。
     *
     * @param project 当前 IDEA 项目
     * @return 分支偏好服务实例
     */
    public static BranchPreferenceService getInstance(Project project) {
        return project.getService(BranchPreferenceService.class);
    }

    @Override
    public @Nullable StateBean getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateBean state) {
        this.state = state;
    }

    /**
     * 返回最近使用过的目标分支列表。
     *
     * @return 最近目标分支列表
     */
    public List<String> getRecentTargetBranches() {
        return List.copyOf(state.recentTargetBranches);
    }

    /**
     * 记录最近使用的目标分支。
     *
     * @param targetBranch 目标分支名称
     */
    public void rememberTargetBranch(String targetBranch) {
        if (targetBranch == null || targetBranch.trim().isEmpty()) {
            return;
        }
        String normalizedTargetBranch = targetBranch.trim();
        state.recentTargetBranches.remove(normalizedTargetBranch);
        state.recentTargetBranches.add(0, normalizedTargetBranch);
        if (state.recentTargetBranches.size() > 10) {
            state.recentTargetBranches = new ArrayList<>(state.recentTargetBranches.subList(0, 10));
        }
    }

    /**
     * 返回主分支候选顺序。
     *
     * @return 主分支候选顺序
     */
    public List<String> getMainBranchNames() {
        if (state.mainBranchNames.isEmpty()) {
            state.mainBranchNames = new ArrayList<>(List.of("main", "master"));
        }
        return List.copyOf(state.mainBranchNames);
    }

    /**
     * 设置主分支候选顺序。
     *
     * @param mainBranchNames 主分支候选顺序
     */
    public void setMainBranchNames(List<String> mainBranchNames) {
        if (mainBranchNames == null || mainBranchNames.isEmpty()) {
            state.mainBranchNames = new ArrayList<>(List.of("main", "master"));
            return;
        }
        List<String> normalizedMainBranchNames = mainBranchNames.stream()
                .filter(item -> item != null && !item.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
        state.mainBranchNames = new ArrayList<>(normalizedMainBranchNames);
    }

    /**
     * 返回默认变更保护模式。
     *
     * @return 默认变更保护模式
     */
    public ChangeProtectionMode getDefaultChangeProtectionMode() {
        return state.defaultChangeProtectionMode;
    }

    /**
     * 设置默认变更保护模式。
     *
     * @param defaultChangeProtectionMode 默认变更保护模式
     */
    public void setDefaultChangeProtectionMode(ChangeProtectionMode defaultChangeProtectionMode) {
        state.defaultChangeProtectionMode = defaultChangeProtectionMode == null
                ? ChangeProtectionMode.ASK
                : defaultChangeProtectionMode;
    }

    /**
     * 返回最近刷新时间。
     *
     * @return 最近刷新时间；若从未刷新则返回空字符串
     */
    public String getLastRefreshAt() {
        return state.lastRefreshAt;
    }

    /**
     * 用当前时间更新最近刷新时间。
     */
    public void updateLastRefreshAt() {
        state.lastRefreshAt = Instant.now().toString();
    }

    /**
     * 判断目标分支名称是否合法。
     *
     * @param targetBranch 目标分支名称
     * @return 合法返回 {@code true}
     */
    public boolean isValidTargetBranch(String targetBranch) {
        return targetBranch != null
                && !targetBranch.trim().isEmpty()
                && !INVALID_BRANCH_PATTERN.matcher(targetBranch.trim()).find();
    }

    /**
     * 按项目偏好返回主分支候选顺序。
     *
     * @return 主分支候选顺序
     */
    public List<String> getMainBranchCandidates() {
        return getMainBranchNames();
    }

    /**
     * 表示可持久化的偏好状态对象。
     *
     * @author August Lee
     */
    public static final class StateBean {

        /**
         * 最近使用过的目标分支。
         */
        public List<String> recentTargetBranches = new ArrayList<>();

        /**
         * 主分支候选顺序。
         */
        public List<String> mainBranchNames = new ArrayList<>(List.of("main", "master"));

        /**
         * 默认变更保护模式。
         */
        public ChangeProtectionMode defaultChangeProtectionMode = ChangeProtectionMode.ASK;

        /**
         * 最近刷新时间。
         */
        public String lastRefreshAt = "";
    }
}
