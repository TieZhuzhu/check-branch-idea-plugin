package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * 负责扫描当前 IDEA 工作区中的 Git 仓库，并读取基础分支状态信息。
 *
 * @author August Lee
 */
public class RepositoryDiscoveryService {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages.CheckBranchBundle");

    /**
     * 扫描当前 IDEA 项目中可识别的 Git 仓库。
     *
     * @param project 当前 IDEA 项目
     * @return 仓库列表
     */
    public List<WorkspaceRepository> discoverRepositories(Project project) {
        Map<String, Path> repositoryPaths = new LinkedHashMap<>();

        for (VirtualFile contentRoot : ProjectRootManager.getInstance(project).getContentRoots()) {
            Path contentRootPath = Path.of(contentRoot.getPath());
            collectGitRepositories(contentRootPath, repositoryPaths);
        }

        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        for (VirtualFile vcsRoot : vcsManager.getRootsUnderVcs(vcsManager.findVcsByName("Git"))) {
            repositoryPaths.putIfAbsent(vcsRoot.getPath(), Path.of(vcsRoot.getPath()));
        }

        List<WorkspaceRepository> repositories = repositoryPaths.values().stream()
                .sorted(Comparator.comparing(Path::toString))
                .map(this::buildRepositorySnapshot)
                .toList();

        BranchPreferenceService.getInstance(project).updateLastRefreshAt();
        return repositories;
    }

    /**
     * 为测试或非 IDEA 调用方扫描指定目录下的 Git 仓库。
     *
     * @param roots 待扫描的根目录列表
     * @return 仓库列表
     */
    public List<WorkspaceRepository> discoverRepositoriesFromPaths(List<Path> roots) {
        Map<String, Path> repositoryPaths = new LinkedHashMap<>();
        if (roots == null) {
            return List.of();
        }
        for (Path root : roots) {
            collectGitRepositories(root, repositoryPaths);
        }
        return repositoryPaths.values().stream()
                .sorted(Comparator.comparing(Path::toString))
                .map(this::buildRepositorySnapshot)
                .toList();
    }

    /**
     * 将指定路径解析为工作区仓库快照。
     *
     * @param repositoryRoot 仓库根路径
     * @return 工作区仓库快照
     */
    public WorkspaceRepository buildRepositorySnapshot(Path repositoryRoot) {
        String normalizedRootPath = repositoryRoot.toAbsolutePath().normalize().toString();
        WorkspaceRepository repository = new WorkspaceRepository(
                normalizedRootPath,
                repositoryRoot.getFileName() == null ? normalizedRootPath : repositoryRoot.getFileName().toString(),
                normalizedRootPath
        );
        repository.setSelected(false);

        if (!Files.isDirectory(repositoryRoot.resolve(".git"))) {
            repository.setOperationBlocked(true);
            repository.setBlockReason(BUNDLE.getString("status.repository.invalid"));
            repository.setTargetBranchState(BUNDLE.getString("status.branch.unknown"));
            repository.setLastOperationResultSummary(BUNDLE.getString("status.result.none"));
            return repository;
        }

        repository.setCurrentBranch(readCurrentBranch(repositoryRoot));
        repository.setMainBranchCandidate(readMainBranchCandidate(repositoryRoot));
        repository.setHasUncommittedChanges(hasUncommittedChanges(repositoryRoot));
        repository.setTargetBranchState(BUNDLE.getString("status.branch.local"));
        repository.setLastOperationResultSummary(BUNDLE.getString("status.result.none"));

        String blockReason = detectBlockReason(repositoryRoot);
        repository.setOperationBlocked(!blockReason.isEmpty());
        repository.setBlockReason(blockReason);
        repository.validate();
        return repository;
    }

    private void collectGitRepositories(Path root, Map<String, Path> repositoryPaths) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        if (Files.isDirectory(root.resolve(".git"))) {
            repositoryPaths.putIfAbsent(root.toAbsolutePath().normalize().toString(), root);
            return;
        }
        try (Stream<Path> pathStream = Files.walk(root, 4)) {
            pathStream
                    .filter(path -> Files.isDirectory(path.resolve(".git")))
                    .forEach(path -> repositoryPaths.putIfAbsent(path.toAbsolutePath().normalize().toString(), path));
        } catch (IOException ignored) {
            // 阶段 3 保持最小实现：读取失败的目录直接跳过，界面层再统一做刷新失败提示。
        }
    }

    private String readCurrentBranch(Path repositoryRoot) {
        Path headFile = repositoryRoot.resolve(".git").resolve("HEAD");
        if (!Files.exists(headFile)) {
            return BUNDLE.getString("status.branch.unknown");
        }
        try {
            String headContent = Files.readString(headFile, StandardCharsets.UTF_8).trim();
            if (headContent.startsWith("ref:")) {
                String normalizedReference = headContent.substring(4).trim();
                String prefix = "refs/heads/";
                if (normalizedReference.startsWith(prefix)) {
                    return normalizedReference.substring(prefix.length());
                }
                return normalizedReference;
            }
            return headContent.isEmpty() ? BUNDLE.getString("status.branch.unknown") : headContent;
        } catch (IOException exception) {
            return BUNDLE.getString("status.branch.unknown");
        }
    }

    private String readMainBranchCandidate(Path repositoryRoot) {
        Path refsHeadsMain = repositoryRoot.resolve(".git").resolve("refs").resolve("heads").resolve("main");
        if (Files.exists(refsHeadsMain)) {
            return "main";
        }
        Path refsHeadsMaster = repositoryRoot.resolve(".git").resolve("refs").resolve("heads").resolve("master");
        if (Files.exists(refsHeadsMaster)) {
            return "master";
        }
        return "";
    }

    private boolean hasUncommittedChanges(Path repositoryRoot) {
        try (Stream<Path> pathStream = Files.walk(repositoryRoot, 3)) {
            return pathStream
                    .filter(path -> !path.startsWith(repositoryRoot.resolve(".git")))
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant()
                                    .isAfter(Instant.now().minusSeconds(30));
                        } catch (IOException exception) {
                            return false;
                        }
                    });
        } catch (IOException exception) {
            return false;
        }
    }

    private String detectBlockReason(Path repositoryRoot) {
        Path gitPath = repositoryRoot.resolve(".git");
        List<PathReasonPair> markers = new ArrayList<>(List.of(
                new PathReasonPair(gitPath.resolve("MERGE_HEAD"), BUNDLE.getString("status.repository.blocked.merge")),
                new PathReasonPair(gitPath.resolve("rebase-apply"), BUNDLE.getString("status.repository.blocked.rebase")),
                new PathReasonPair(gitPath.resolve("rebase-merge"), BUNDLE.getString("status.repository.blocked.rebase")),
                new PathReasonPair(gitPath.resolve("CHERRY_PICK_HEAD"), BUNDLE.getString("status.repository.blocked.cherry-pick"))
        ));

        for (PathReasonPair marker : markers) {
            if (Files.exists(marker.path())) {
                return marker.reason();
            }
        }

        Path indexLock = gitPath.resolve("index.lock");
        if (Files.exists(indexLock)) {
            return BUNDLE.getString("status.repository.blocked.conflict");
        }
        return "";
    }

    private record PathReasonPair(Path path, String reason) {
    }
}
