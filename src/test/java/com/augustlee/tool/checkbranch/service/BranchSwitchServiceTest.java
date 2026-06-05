package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.BranchSwitchRequest;
import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
import com.augustlee.tool.checkbranch.model.SwitchResult;
import com.augustlee.tool.checkbranch.model.SwitchResultStatus;
import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验分支切换服务的目标分支命中、远端命中与失败路径。
 *
 * @author August Lee
 */
class BranchSwitchServiceTest {

    private final BranchSwitchService branchSwitchService = new BranchSwitchService();

    /**
     * 验证本地目标分支存在时可以直接切换成功。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldSwitchToExistingLocalBranch(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createGitRepository(tempDir, tempDir.resolve("service-order"), "main");
        runGit(repository.getRootPath(), "checkout", "-b", "feature/demo");
        runGit(repository.getRootPath(), "checkout", "main");

        SwitchResult result = branchSwitchService.switchRepository(repository, "feature/demo", List.of("main", "master"));

        assertEquals(SwitchResultStatus.SUCCESS, result.getStatus());
        assertEquals("feature/demo", result.getFinalBranch());
        assertEquals("feature/demo", repository.getCurrentBranch());
        assertEquals("本地分支", result.getBranchSource());
    }

    /**
     * 验证分支引用被 Git 打包到 packed-refs 后仍能识别和切换。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldResolvePackedBranchReferences(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createGitRepository(tempDir, tempDir.resolve("service-packed"), "main");
        runGit(repository.getRootPath(), "checkout", "-b", "feature/packed");
        runGit(repository.getRootPath(), "checkout", "main");
        runGit(repository.getRootPath(), "pack-refs", "--all");

        assertTrue(branchSwitchService.localBranchExists(Path.of(repository.getRootPath()), "feature/packed"));

        SwitchResult result = branchSwitchService.switchRepository(repository, "feature/packed", List.of("main", "master"));

        assertEquals(SwitchResultStatus.SUCCESS, result.getStatus());
        assertEquals("feature/packed", repository.getCurrentBranch());
    }

    /**
     * 验证远端分支存在但远端 checkout 失败时，会降级为本地分支切换。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldFallbackToLocalBranchWhenRemoteCheckoutFails(@TempDir Path tempDir) throws IOException {
        Path remoteRoot = tempDir.resolve("broken-remote");
        createBareRemoteRepositoryWithMain(tempDir, remoteRoot);
        Path localRoot = tempDir.resolve("service-broken-remote");
        runGit(tempDir.toString(), "clone", "-b", "main", remoteRoot.toString(), localRoot.toString());
        runGit(localRoot.toString(), "checkout", "-b", "feature/demo");
        runGit(localRoot.toString(), "commit", "--allow-empty", "-m", "feature-demo");
        runGit(localRoot.toString(), "push", "-u", "origin", "feature/demo");
        runGit(localRoot.toString(), "checkout", "main");
        runGit(localRoot.toString(), "remote", "set-url", "origin", tempDir.resolve("missing-remote").toString());
        WorkspaceRepository repository = openWorkspaceRepository(localRoot);

        SwitchResult result = branchSwitchService.switchRepository(repository, "feature/demo", List.of("main", "master"));

        assertEquals(SwitchResultStatus.SUCCESS, result.getStatus());
        assertEquals("本地分支", result.getBranchSource());
        assertEquals("feature/demo", repository.getCurrentBranch());
    }

    /**
     * 验证仓库已经处在目标分支时直接跳过，不再执行冗余切换。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldSkipWhenRepositoryAlreadyOnTargetBranch(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createGitRepository(tempDir, tempDir.resolve("service-current"), "main");

        SwitchResult result = branchSwitchService.switchRepository(repository, "main", List.of("main", "master"));

        assertEquals(SwitchResultStatus.SKIPPED, result.getStatus());
        assertEquals("当前已经处在目标分支，已跳过切换", result.getMessage());
        assertEquals("main", repository.getCurrentBranch());
    }

    /**
     * 验证切换前存在未提交变更时会先临时保存，并把记录关联到结果。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldProtectChangesBeforeSwitch(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createGitRepository(tempDir, tempDir.resolve("service-dirty"), "main");
        runGit(repository.getRootPath(), "checkout", "-b", "feature/demo");
        runGit(repository.getRootPath(), "checkout", "main");
        Files.writeString(Path.of(repository.getRootPath()).resolve("README.md"), "# changed", StandardCharsets.UTF_8);
        repository.setHasUncommittedChanges(true);

        SwitchResult result = branchSwitchService.switchRepository(
                repository,
                "feature/demo",
                List.of("main", "master"),
                ChangeProtectionMode.GIT_STASH
        );

        assertEquals(SwitchResultStatus.MANUAL_RESTORE_REQUIRED, result.getStatus());
        assertEquals("feature/demo", result.getFinalBranch());
        assertTrue(!result.getTemporaryChangeRecordId().isBlank());
        assertTrue(runGit(repository.getRootPath(), "stash", "list").contains("检查分支插件临时保存"));
    }

    /**
     * 验证仅存在远端分支时仍可以切换成功。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldSwitchToRemoteBranchWhenLocalBranchMissing(@TempDir Path tempDir) throws IOException {
        Path remoteRoot = tempDir.resolve("service-user-remote");
        createBareRemoteRepositoryWithMain(tempDir, remoteRoot);
        Path localRoot = tempDir.resolve("service-user");
        runGit(tempDir.toString(), "clone", "-b", "main", remoteRoot.toString(), localRoot.toString());
        runGit(localRoot.toString(), "checkout", "-b", "feature/remote-demo");
        runGit(localRoot.toString(), "commit", "--allow-empty", "-m", "remote-demo");
        runGit(localRoot.toString(), "push", "-u", "origin", "feature/remote-demo");
        runGit(localRoot.toString(), "checkout", "main");
        runGit(localRoot.toString(), "branch", "-D", "feature/remote-demo");
        WorkspaceRepository repository = openWorkspaceRepository(localRoot);

        SwitchResult result = branchSwitchService.switchRepository(repository, "feature/remote-demo", List.of("main", "master"));

        assertEquals(SwitchResultStatus.SUCCESS, result.getStatus());
        assertEquals("远端分支", result.getBranchSource());
        assertEquals("feature/remote-demo", repository.getCurrentBranch());
        assertEquals("feature/remote-demo", runGit(repository.getRootPath(), "rev-parse", "--abbrev-ref", "HEAD"));
    }

    /**
     * 验证阻塞仓库会直接失败。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldFailWhenRepositoryIsBlocked(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createGitRepository(tempDir, tempDir.resolve("service-pay"), "main");
        repository.setOperationBlocked(true);
        repository.setBlockReason("正在变基");

        SwitchResult result = branchSwitchService.switchRepository(repository, "feature/demo", List.of("main", "master"));

        assertEquals(SwitchResultStatus.FAILED, result.getStatus());
        assertEquals("正在变基", result.getFailureReason());
        assertTrue(result.getFinalBranch().isEmpty());
    }

    /**
     * 验证批量切换可以返回逐仓库结果。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldSwitchMultipleRepositories(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository serviceOrder = createGitRepository(tempDir, tempDir.resolve("service-order"), "main");
        runGit(serviceOrder.getRootPath(), "checkout", "-b", "feature/demo");
        runGit(serviceOrder.getRootPath(), "checkout", "main");
        WorkspaceRepository serviceUser = createGitRepository(tempDir, tempDir.resolve("service-user"), "main");

        BranchSwitchRequest request = new BranchSwitchRequest(
                "request-1",
                "feature/demo",
                List.of(serviceOrder.getId(), serviceUser.getId()),
                ChangeProtectionMode.ASK,
                Instant.now(),
                false
        );

        List<SwitchResult> results = branchSwitchService.switchBranches(
                request,
                List.of(serviceOrder, serviceUser),
                List.of("main", "master")
        );

        assertEquals(2, results.size());
        assertEquals(SwitchResultStatus.SUCCESS, results.get(0).getStatus());
        assertEquals(SwitchResultStatus.FALLBACK_TO_MAIN, results.get(1).getStatus());
    }

    /**
     * 验证目标分支缺失时可以回退到 master。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldFallbackToMasterWhenMainMissing(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createGitRepository(tempDir, tempDir.resolve("service-master"), "master");

        SwitchResult result = branchSwitchService.switchRepository(repository, "feature/missing", List.of("main", "master"));

        assertEquals(SwitchResultStatus.FALLBACK_TO_MAIN, result.getStatus());
        assertEquals("master", result.getFinalBranch());
        assertEquals("master", repository.getCurrentBranch());
    }

    private WorkspaceRepository createGitRepository(Path tempDir, Path repositoryRoot, String branchName) throws IOException {
        runGit(tempDir.toString(), "init", repositoryRoot.toString());
        runGit(repositoryRoot.toString(), "config", "user.name", "codex");
        runGit(repositoryRoot.toString(), "config", "user.email", "codex@example.com");
        Files.writeString(repositoryRoot.resolve("README.md"), "# test", StandardCharsets.UTF_8);
        runGit(repositoryRoot.toString(), "add", ".");
        runGit(repositoryRoot.toString(), "commit", "-m", "init");
        if (!"master".equals(branchName)) {
            runGit(repositoryRoot.toString(), "branch", "-M", branchName);
        }
        return openWorkspaceRepository(repositoryRoot);
    }

    private WorkspaceRepository openWorkspaceRepository(Path repositoryRoot) throws IOException {
        WorkspaceRepository repository = new WorkspaceRepository(repositoryRoot.toString(), repositoryRoot.getFileName().toString(), repositoryRoot.toString());
        repository.setCurrentBranch(runGit(repositoryRoot.toString(), "rev-parse", "--abbrev-ref", "HEAD"));
        repository.setTargetBranchState("可切换");
        repository.setOperationBlocked(false);
        repository.setLastOperationResultSummary("暂无结果");
        return repository;
    }

    private void createBareRemoteRepositoryWithMain(Path tempDir, Path remoteRoot) throws IOException {
        Path seedRoot = tempDir.resolve("service-user-seed");
        createGitRepository(tempDir, seedRoot, "main");
        runGit(seedRoot.toString(), "remote", "add", "origin", remoteRoot.toString());
        runGit(tempDir.toString(), "init", "--bare", remoteRoot.toString());
        runGit(seedRoot.toString(), "push", "-u", "origin", "main");
        runGit(remoteRoot.toString(), "symbolic-ref", "HEAD", "refs/heads/main");
    }

    private String runGit(String workingDirectory, String... command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(command));
        processBuilder.directory(Path.of(workingDirectory).toFile());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Git 命令失败：" + String.join(" ", command) + System.lineSeparator() + output);
            }
            return output;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git 命令执行被中断", exception);
        }
    }

    private String runGit(Path workingDirectory, String... command) throws IOException {
        return runGit(workingDirectory.toString(), command);
    }

    private List<String> buildCommand(String... command) {
        List<String> fullCommand = new java.util.ArrayList<>();
        fullCommand.add("git");
        fullCommand.addAll(List.of(command));
        return fullCommand;
    }
}
