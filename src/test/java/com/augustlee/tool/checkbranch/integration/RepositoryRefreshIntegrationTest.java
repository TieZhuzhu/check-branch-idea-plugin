package com.augustlee.tool.checkbranch.integration;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import com.augustlee.tool.checkbranch.service.RepositoryDiscoveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用临时多仓库目录验证刷新场景下的仓库扫描结果。
 *
 * @author August Lee
 */
class RepositoryRefreshIntegrationTest {

    /**
     * 验证刷新时可以同时识别多个仓库与不同状态。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldRefreshRepositoriesFromWorkspaceRoots(@TempDir Path tempDir) throws IOException {
        Path serviceOrder = createRepository(tempDir.resolve("service-order"), "ref: refs/heads/main");
        Path serviceUser = createRepository(tempDir.resolve("service-user"), "ref: refs/heads/feature/demo");
        Path servicePay = createRepository(tempDir.resolve("service-pay"), "ref: refs/heads/main");
        Files.writeString(servicePay.resolve(".git").resolve("rebase-apply"), "rebase", StandardCharsets.UTF_8);

        RepositoryDiscoveryService repositoryDiscoveryService = new RepositoryDiscoveryService();
        List<WorkspaceRepository> repositories = repositoryDiscoveryService.discoverRepositoriesFromPaths(
                List.of(serviceOrder, serviceUser, servicePay)
        );

        assertEquals(3, repositories.size());
        assertEquals("service-order", repositories.get(0).getDisplayName());
        assertEquals("service-pay", repositories.get(1).getDisplayName());
        assertEquals("service-user", repositories.get(2).getDisplayName());
        assertTrue(repositories.stream().anyMatch(WorkspaceRepository::isOperationBlocked));
    }

    private Path createRepository(Path repositoryRoot, String headContent) throws IOException {
        Files.createDirectories(repositoryRoot.resolve(".git").resolve("refs").resolve("heads"));
        Files.writeString(repositoryRoot.resolve(".git").resolve("HEAD"), headContent, StandardCharsets.UTF_8);
        Files.writeString(repositoryRoot.resolve("application.yml"), "server:\n  port: 8080", StandardCharsets.UTF_8);
        return repositoryRoot;
    }
}
