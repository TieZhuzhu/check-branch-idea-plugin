package com.augustlee.tool.checkbranch.integration;

import com.augustlee.tool.checkbranch.model.BranchSwitchRequest;
import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
import com.augustlee.tool.checkbranch.model.SwitchResult;
import com.augustlee.tool.checkbranch.model.SwitchResultStatus;
import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import com.augustlee.tool.checkbranch.service.BranchSwitchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用真实临时 Git 仓库验证未提交变更保护与分支切换串联行为。
 *
 * @author August Lee
 */
class ChangeProtectionIntegrationTest {

    /**
     * 验证未提交变更会先进入 Git 暂存栈，再继续切换到目标分支。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldStashDirtyRepositoryBeforeSwitchingBranch(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepository(tempDir, tempDir.resolve("dirty-repo"), "main");
        runGit(repository.getRootPath(), "checkout", "-b", "feature/demo");
        runGit(repository.getRootPath(), "checkout", "main");
        Files.writeString(Path.of(repository.getRootPath()).resolve("README.md"), "# changed", StandardCharsets.UTF_8);
        repository.setHasUncommittedChanges(true);

        BranchSwitchRequest request = new BranchSwitchRequest(
                "change-protection-request",
                "feature/demo",
                List.of(repository.getId()),
                ChangeProtectionMode.ASK,
                Instant.now(),
                false
        );

        List<SwitchResult> results = new BranchSwitchService().switchBranches(
                request,
                List.of(repository),
                List.of("main", "master"),
                Map.of(repository.getId(), ChangeProtectionMode.GIT_STASH)
        );

        assertEquals(1, results.size());
        assertEquals(SwitchResultStatus.MANUAL_RESTORE_REQUIRED, results.get(0).getStatus());
        assertEquals("feature/demo", runGit(repository.getRootPath(), "rev-parse", "--abbrev-ref", "HEAD"));
        assertFalse(results.get(0).getTemporaryChangeRecordId().isBlank());
        assertTrue(runGit(repository.getRootPath(), "stash", "list").contains("检查分支插件临时保存"));
    }

    /**
     * 验证默认询问模式在后台执行时不会自动降级 Git 暂存栈。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldNotAutomaticallyFallbackToGitStashForAskMode(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepository(tempDir, tempDir.resolve("ask-dirty-repo"), "main");
        runGit(repository.getRootPath(), "checkout", "-b", "feature/demo");
        runGit(repository.getRootPath(), "checkout", "main");
        Files.writeString(Path.of(repository.getRootPath()).resolve("README.md"), "# changed", StandardCharsets.UTF_8);
        repository.setHasUncommittedChanges(true);

        BranchSwitchRequest request = new BranchSwitchRequest(
                "ask-change-protection-request",
                "feature/demo",
                List.of(repository.getId()),
                ChangeProtectionMode.ASK,
                Instant.now(),
                false
        );

        List<SwitchResult> results = new BranchSwitchService().switchBranches(
                request,
                List.of(repository),
                List.of("main", "master")
        );

        assertEquals(SwitchResultStatus.FAILED, results.get(0).getStatus());
        assertTrue(results.get(0).getFailureReason().contains("当前后台切换流程暂不能调用 IDEA 搁置变更"));
        assertEquals("main", runGit(repository.getRootPath(), "rev-parse", "--abbrev-ref", "HEAD"));
        assertTrue(runGit(repository.getRootPath(), "stash", "list").isBlank());
    }

    /**
     * 验证用户选择跳过时不会暂存，也不会切换当前仓库。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldSkipDirtyRepositoryWhenUserChoosesSkip(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepository(tempDir, tempDir.resolve("skip-dirty-repo"), "main");
        runGit(repository.getRootPath(), "checkout", "-b", "feature/demo");
        runGit(repository.getRootPath(), "checkout", "main");
        Files.writeString(Path.of(repository.getRootPath()).resolve("README.md"), "# changed", StandardCharsets.UTF_8);
        repository.setHasUncommittedChanges(true);

        BranchSwitchRequest request = new BranchSwitchRequest(
                "skip-change-request",
                "feature/demo",
                List.of(repository.getId()),
                ChangeProtectionMode.ASK,
                Instant.now(),
                false
        );

        List<SwitchResult> results = new BranchSwitchService().switchBranches(
                request,
                List.of(repository),
                List.of("main", "master"),
                Map.of(repository.getId(), ChangeProtectionMode.SKIP)
        );

        assertEquals(SwitchResultStatus.SKIPPED, results.get(0).getStatus());
        assertEquals("main", runGit(repository.getRootPath(), "rev-parse", "--abbrev-ref", "HEAD"));
        assertTrue(runGit(repository.getRootPath(), "status", "--porcelain").contains("README.md"));
    }

    private WorkspaceRepository createRepository(Path tempDir, Path repositoryRoot, String branchName) throws IOException {
        runGit(tempDir.toString(), "init", repositoryRoot.toString());
        runGit(repositoryRoot.toString(), "config", "user.name", "codex");
        runGit(repositoryRoot.toString(), "config", "user.email", "codex@example.com");
        Files.writeString(repositoryRoot.resolve("README.md"), "# test", StandardCharsets.UTF_8);
        runGit(repositoryRoot.toString(), "add", ".");
        runGit(repositoryRoot.toString(), "commit", "-m", "init");
        if (!"master".equals(branchName)) {
            runGit(repositoryRoot.toString(), "branch", "-M", branchName);
        }
        WorkspaceRepository repository = new WorkspaceRepository(
                repositoryRoot.toString(),
                repositoryRoot.getFileName().toString(),
                repositoryRoot.toString()
        );
        repository.setCurrentBranch(runGit(repositoryRoot.toString(), "rev-parse", "--abbrev-ref", "HEAD"));
        repository.setTargetBranchState("可切换");
        repository.setLastOperationResultSummary("暂无结果");
        return repository;
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

    private List<String> buildCommand(String... command) {
        List<String> fullCommand = new java.util.ArrayList<>();
        fullCommand.add("git");
        fullCommand.addAll(List.of(command));
        return fullCommand;
    }
}
