package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验仓库发现服务的目录扫描与 Git 状态读取行为。
 *
 * @author August Lee
 */
class RepositoryDiscoveryServiceTest {

    private final RepositoryDiscoveryService repositoryDiscoveryService = new RepositoryDiscoveryService();

    /**
     * 验证服务能够扫描多个 Git 仓库并读取分支与主分支候选信息。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试目录初始化失败时抛出
     */
    @Test
    void shouldDiscoverMultipleGitRepositories(@TempDir Path tempDir) throws IOException {
        Path serviceOrder = createGitRepository(tempDir.resolve("service-order"), "ref: refs/heads/main");
        Path serviceUser = createGitRepository(tempDir.resolve("service-user"), "ref: refs/heads/feature/demo");
        Files.createDirectories(serviceUser.resolve(".git").resolve("refs").resolve("heads"));
        Files.writeString(
                serviceUser.resolve(".git").resolve("refs").resolve("heads").resolve("master"),
                "hash-master",
                StandardCharsets.UTF_8
        );

        List<WorkspaceRepository> repositories = repositoryDiscoveryService.discoverRepositoriesFromPaths(List.of(tempDir));

        assertEquals(2, repositories.size());
        assertEquals("main", repositories.get(0).getCurrentBranch());
        assertEquals("feature/demo", repositories.get(1).getCurrentBranch());
        assertEquals("main", repositories.get(0).getMainBranchCandidate());
        assertEquals("master", repositories.get(1).getMainBranchCandidate());
        assertFalse(repositories.get(0).isOperationBlocked());
    }

    /**
     * 验证服务能够识别阻塞状态与不可识别仓库。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试目录初始化失败时抛出
     */
    @Test
    void shouldMarkBlockedAndInvalidRepositories(@TempDir Path tempDir) throws IOException {
        Path blockedRepository = createGitRepository(tempDir.resolve("service-pay"), "ref: refs/heads/main");
        Files.writeString(blockedRepository.resolve(".git").resolve("MERGE_HEAD"), "merge", StandardCharsets.UTF_8);

        WorkspaceRepository blockedSnapshot = repositoryDiscoveryService.buildRepositorySnapshot(blockedRepository);
        WorkspaceRepository invalidSnapshot = repositoryDiscoveryService.buildRepositorySnapshot(tempDir.resolve("not-a-repo"));

        assertTrue(blockedSnapshot.isOperationBlocked());
        assertEquals("正在合并", blockedSnapshot.getBlockReason());
        assertTrue(invalidSnapshot.isOperationBlocked());
        assertEquals("不可识别 Git 仓库", invalidSnapshot.getBlockReason());
    }

    private Path createGitRepository(Path repositoryRoot, String headContent) throws IOException {
        Files.createDirectories(repositoryRoot.resolve(".git").resolve("refs").resolve("heads"));
        Files.writeString(repositoryRoot.resolve(".git").resolve("HEAD"), headContent, StandardCharsets.UTF_8);
        String branchName = headContent.substring(headContent.lastIndexOf('/') + 1);
        Files.writeString(
                repositoryRoot.resolve(".git").resolve("refs").resolve("heads").resolve(branchName),
                "hash-" + branchName,
                StandardCharsets.UTF_8
        );
        Files.writeString(repositoryRoot.resolve("README.md"), "# test", StandardCharsets.UTF_8);
        return repositoryRoot;
    }
}
