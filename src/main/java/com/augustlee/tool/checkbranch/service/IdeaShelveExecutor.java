package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager;
import com.intellij.openapi.vcs.changes.shelf.ShelvedChangeList;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

/**
 * 使用 IDEA 原生 Shelf 能力搁置指定 Git 仓库范围内的变更。
 *
 * @author August Lee
 */
public class IdeaShelveExecutor implements ChangeProtectionService.ShelveExecutor {

    private static final DateTimeFormatter SHELVE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Project project;

    /**
     * 创建 IDEA Shelf 执行器。
     *
     * @param project 当前 IDEA 项目
     */
    public IdeaShelveExecutor(Project project) {
        this.project = project;
    }

    @Override
    public ChangeProtectionService.ProtectionCommandResult shelve(WorkspaceRepository repository) {
        if (project == null || project.isDisposed()) {
            return ChangeProtectionService.ProtectionCommandResult.failed("当前 IDEA 项目不可用，无法执行搁置变更");
        }
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> projectChanges = changeListManager.getAllChanges();
        List<Change> repositoryChanges = projectChanges.stream()
                .filter(change -> belongsToRepository(change, Path.of(repository.getRootPath())))
                .toList();
        if (repositoryChanges.isEmpty()) {
            return ChangeProtectionService.ProtectionCommandResult.failed("IDEA 未在该仓库范围内发现可搁置的已跟踪变更");
        }

        String shelveName = "检查分支插件搁置记录 - "
                + repository.getDisplayName()
                + " - "
                + SHELVE_TIME_FORMATTER.format(Instant.now());
        try {
            // 第一个 true 表示搁置成功后回滚工作区变更，保证后续分支切换不会被本地修改阻塞；
            // 第二个 false 保持 IDEA Shelf 默认粒度，不额外强制二进制文本内容写入策略。
            ShelvedChangeList shelvedChangeList = ShelveChangesManager.getInstance(project)
                    .shelveChanges(repositoryChanges, shelveName, true, false);
            return ChangeProtectionService.ProtectionCommandResult.success(shelvedChangeList.getName());
        } catch (IOException | VcsException exception) {
            return ChangeProtectionService.ProtectionCommandResult.failed("IDEA 搁置变更失败：" + exception.getMessage());
        } catch (RuntimeException exception) {
            return ChangeProtectionService.ProtectionCommandResult.failed("IDEA 搁置变更异常：" + exception.getMessage());
        }
    }

    private boolean belongsToRepository(Change change, Path repositoryRoot) {
        return revisionBelongsToRepository(change.getBeforeRevision(), repositoryRoot)
                || revisionBelongsToRepository(change.getAfterRevision(), repositoryRoot);
    }

    private boolean revisionBelongsToRepository(ContentRevision revision, Path repositoryRoot) {
        if (revision == null || revision.getFile() == null) {
            return false;
        }
        Path changePath = revision.getFile().getIOFile().toPath();
        return changePath.normalize().startsWith(repositoryRoot.normalize());
    }
}
