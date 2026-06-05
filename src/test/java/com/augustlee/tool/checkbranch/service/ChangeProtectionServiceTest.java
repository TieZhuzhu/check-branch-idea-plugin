package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验未提交变更保护服务的检测、搁置和暂存降级路径。
 *
 * @author August Lee
 */
class ChangeProtectionServiceTest {

    /**
     * 验证干净工作区不创建临时变更记录。
     *
     * @param tempDir 临时目录
     * @throws IOException 当 Git 仓库初始化失败时抛出
     */
    @Test
    void shouldReturnCleanWhenRepositoryHasNoChanges(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepository(tempDir, tempDir.resolve("clean-repo"));
        ChangeProtectionService service = new ChangeProtectionService();

        ChangeProtectionService.ChangeProtectionResult result = service.protectChanges(
                repository,
                ChangeProtectionMode.SHELVE,
                true
        );

        assertTrue(result.isClean());
        assertFalse(result.isProtected());
    }

    /**
     * 验证搁置路径成功时会生成手动恢复记录。
     *
     * @param tempDir 临时目录
     * @throws IOException 当 Git 仓库初始化失败时抛出
     */
    @Test
    void shouldShelveChangesWhenShelveExecutorSucceeds(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepositoryWithChanges(tempDir, tempDir.resolve("shelve-repo"));
        ChangeProtectionService service = new ChangeProtectionService(
                repo -> ChangeProtectionService.ProtectionCommandResult.success("已搁置")
        );

        ChangeProtectionService.ChangeProtectionResult result = service.protectChanges(
                repository,
                ChangeProtectionMode.SHELVE,
                true
        );

        assertTrue(result.isProtected());
        assertNotNull(result.getRecord());
        assertEquals(ChangeProtectionMode.SHELVE, result.getRecord().getMethod());
    }

    /**
     * 验证搁置失败且允许降级时会使用真实 Git 暂存栈保存变更。
     *
     * @param tempDir 临时目录
     * @throws IOException 当 Git 仓库初始化失败时抛出
     */
    @Test
    void shouldFallbackToGitStashWhenShelveFails(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepositoryWithChanges(tempDir, tempDir.resolve("stash-repo"));
        ChangeProtectionService service = new ChangeProtectionService(
                repo -> ChangeProtectionService.ProtectionCommandResult.failed("搁置不可用")
        );

        ChangeProtectionService.ChangeProtectionResult result = service.protectChanges(
                repository,
                ChangeProtectionMode.SHELVE,
                true
        );

        assertTrue(result.isProtected());
        assertEquals(ChangeProtectionMode.GIT_STASH, result.getRecord().getMethod());
        assertFalse(service.hasUncommittedChanges(Path.of(repository.getRootPath())));
        assertTrue(runGit(repository.getRootPath(), "stash", "list").contains("检查分支插件临时保存"));
    }

    /**
     * 验证只有未跟踪文件时不触发变更保护，保持与 IDEA 分支切换体验一致。
     *
     * @param tempDir 临时目录
     * @throws IOException 当 Git 仓库初始化失败时抛出
     */
    @Test
    void shouldIgnorePureUntrackedFilesForProtection(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepository(tempDir, tempDir.resolve("untracked-repo"));
        Files.createDirectories(Path.of(repository.getRootPath()).resolve("src/main/resources"));
        Files.writeString(Path.of(repository.getRootPath()).resolve("src/main/resources/rebel.xml"), "<rebel/>", StandardCharsets.UTF_8);
        ChangeProtectionService service = new ChangeProtectionService();

        ChangeProtectionService.ChangeProtectionResult result = service.protectChanges(
                repository,
                ChangeProtectionMode.SHELVE,
                true
        );

        assertTrue(result.isClean());
        assertFalse(result.isProtected());
    }

    /**
     * 验证用户选择跳过时不会保存变更。
     *
     * @param tempDir 临时目录
     * @throws IOException 当 Git 仓库初始化失败时抛出
     */
    @Test
    void shouldSkipRepositoryWhenUserChoosesSkip(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository repository = createRepositoryWithChanges(tempDir, tempDir.resolve("skip-repo"));
        ChangeProtectionService service = new ChangeProtectionService();

        ChangeProtectionService.ChangeProtectionResult result = service.protectChanges(
                repository,
                ChangeProtectionMode.SKIP,
                true
        );

        assertTrue(result.isSkipped());
        assertTrue(service.hasUncommittedChanges(Path.of(repository.getRootPath())));
    }

    private WorkspaceRepository createRepositoryWithChanges(Path tempDir, Path repositoryRoot) throws IOException {
        WorkspaceRepository repository = createRepository(tempDir, repositoryRoot);
        Files.writeString(repositoryRoot.resolve("README.md"), "# changed", StandardCharsets.UTF_8);
        repository.setHasUncommittedChanges(true);
        return repository;
    }

    private WorkspaceRepository createRepository(Path tempDir, Path repositoryRoot) throws IOException {
        runGit(tempDir.toString(), "init", repositoryRoot.toString());
        runGit(repositoryRoot.toString(), "config", "user.name", "codex");
        runGit(repositoryRoot.toString(), "config", "user.email", "codex@example.com");
        Files.writeString(repositoryRoot.resolve("README.md"), "# test", StandardCharsets.UTF_8);
        runGit(repositoryRoot.toString(), "add", ".");
        runGit(repositoryRoot.toString(), "commit", "-m", "init");
        WorkspaceRepository repository = new WorkspaceRepository(
                repositoryRoot.toString(),
                repositoryRoot.getFileName().toString(),
                repositoryRoot.toString()
        );
        repository.setCurrentBranch(runGit(repositoryRoot.toString(), "rev-parse", "--abbrev-ref", "HEAD"));
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
        fullCommand.addAll(java.util.List.of(command));
        return fullCommand;
    }
}
