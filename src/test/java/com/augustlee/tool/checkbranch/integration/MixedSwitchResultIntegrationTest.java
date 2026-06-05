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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证混合结果场景下的切换结果组合。
 *
 * @author August Lee
 */
class MixedSwitchResultIntegrationTest {

    /**
     * 验证批量切换可以同时产出成功、回退和失败结果。
     *
     * @param tempDir 临时目录
     * @throws IOException 当测试仓库初始化失败时抛出
     */
    @Test
    void shouldProduceMixedSwitchResults(@TempDir Path tempDir) throws IOException {
        WorkspaceRepository serviceOrder = createRepository(tempDir, tempDir.resolve("service-order"), "main");
        WorkspaceRepository serviceUser = createRepository(tempDir, tempDir.resolve("service-user"), "main");
        WorkspaceRepository servicePay = createRepository(tempDir, tempDir.resolve("service-pay"), "main");
        servicePay.setOperationBlocked(true);
        servicePay.setBlockReason("正在变基");

        runGit(serviceOrder.getRootPath(), "checkout", "-b", "feature/demo");
        runGit(serviceOrder.getRootPath(), "checkout", "main");

        BranchSwitchRequest request = new BranchSwitchRequest(
                "mixed-request",
                "feature/demo",
                List.of(serviceOrder.getId(), serviceUser.getId(), servicePay.getId()),
                ChangeProtectionMode.ASK,
                Instant.now(),
                false
        );

        BranchSwitchService branchSwitchService = new BranchSwitchService();
        List<SwitchResult> results = branchSwitchService.switchBranches(
                request,
                List.of(serviceOrder, serviceUser, servicePay),
                List.of("main", "master")
        );

        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(result -> result.getStatus() == SwitchResultStatus.SUCCESS));
        assertTrue(results.stream().anyMatch(result -> result.getStatus() == SwitchResultStatus.FALLBACK_TO_MAIN));
        assertTrue(results.stream().anyMatch(result -> result.getStatus() == SwitchResultStatus.FAILED));
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
        WorkspaceRepository repository = new WorkspaceRepository(repositoryRoot.toString(), repositoryRoot.getFileName().toString(), repositoryRoot.toString());
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
