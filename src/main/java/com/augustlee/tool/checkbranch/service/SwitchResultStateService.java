package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.SwitchResult;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存当前项目会话内最近一次切换结果摘要。
 *
 * @author August Lee
 */
@Service(Service.Level.PROJECT)
public final class SwitchResultStateService {

    private final List<SwitchResult> lastResults = new ArrayList<>();
    private Instant updatedAt;

    /**
     * 返回项目级最近结果状态服务。
     *
     * @param project 当前 IDEA 项目
     * @return 结果状态服务
     */
    public static SwitchResultStateService getInstance(Project project) {
        return project.getService(SwitchResultStateService.class);
    }

    /**
     * 保存最近一次切换结果。
     *
     * @param results 最近一次切换结果列表
     */
    public void saveResults(List<SwitchResult> results) {
        lastResults.clear();
        if (results != null) {
            lastResults.addAll(results);
        }
        updatedAt = Instant.now();
    }

    /**
     * 返回最近一次切换结果。
     *
     * @return 最近一次切换结果副本
     */
    public List<SwitchResult> getLastResults() {
        return new ArrayList<>(lastResults);
    }

    /**
     * 返回最近一次结果更新时间。
     *
     * @return 最近一次更新时间；如果从未保存则返回 {@code null}
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 返回当前结果是否只在项目会话内保留。
     *
     * @return 恒为 {@code true}
     */
    public boolean isProjectSessionOnly() {
        return true;
    }
}
