package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.BranchSwitchRequest;
import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
import com.augustlee.tool.checkbranch.model.SwitchResult;
import com.augustlee.tool.checkbranch.model.SwitchResultStatus;
import com.augustlee.tool.checkbranch.model.TemporaryChangeRecord;
import com.augustlee.tool.checkbranch.model.WorkspaceRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 使用真实 Git 命令执行批量分支切换与主分支回退逻辑。
 *
 * @author August Lee
 */
public class BranchSwitchService {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages.CheckBranchBundle");
    private final ChangeProtectionService changeProtectionService;

    /**
     * 创建分支切换服务。
     */
    public BranchSwitchService() {
        this(new ChangeProtectionService());
    }

    /**
     * 使用指定变更保护服务创建分支切换服务，便于测试覆盖未提交变更路径。
     *
     * @param changeProtectionService 变更保护服务
     */
    public BranchSwitchService(ChangeProtectionService changeProtectionService) {
        this.changeProtectionService = changeProtectionService;
    }

    /**
     * 对选中的仓库执行批量分支切换。
     *
     * @param request 分支切换请求
     * @param repositories 当前仓库快照列表
     * @param mainBranchCandidates 主分支候选顺序
     * @return 逐仓库切换结果
     */
    public List<SwitchResult> switchBranches(
            BranchSwitchRequest request,
            List<WorkspaceRepository> repositories,
            List<String> mainBranchCandidates
    ) {
        return switchBranches(request, repositories, mainBranchCandidates, Map.of());
    }

    /**
     * 对选中的仓库执行批量分支切换，并支持逐仓库指定变更保护模式。
     *
     * @param request 分支切换请求
     * @param repositories 当前仓库快照列表
     * @param mainBranchCandidates 主分支候选顺序
     * @param changeProtectionModes 逐仓库变更保护模式
     * @return 逐仓库切换结果
     */
    public List<SwitchResult> switchBranches(
            BranchSwitchRequest request,
            List<WorkspaceRepository> repositories,
            List<String> mainBranchCandidates,
            Map<String, ChangeProtectionMode> changeProtectionModes
    ) {
        request.validate();
        Map<String, WorkspaceRepository> repositoryMap = repositories.stream()
                .collect(Collectors.toMap(WorkspaceRepository::getId, Function.identity(), (left, right) -> left));
        List<SwitchResult> results = new ArrayList<>();
        for (String repositoryId : request.getRepositoryIds()) {
            WorkspaceRepository repository = repositoryMap.get(repositoryId);
            if (repository == null) {
                results.add(buildFailedResult(repositoryId, request.getTargetBranch(), "仓库不存在或未刷新"));
                continue;
            }
            ChangeProtectionMode mode = changeProtectionModes.getOrDefault(repositoryId, request.getChangeProtectionMode());
            results.add(switchRepository(repository, request.getTargetBranch(), mainBranchCandidates, mode));
        }
        return results;
    }

    /**
     * 对单个仓库执行目标分支切换。
     *
     * @param repository 仓库快照
     * @param targetBranch 目标分支
     * @param mainBranchCandidates 主分支候选顺序
     * @return 单仓库切换结果
     */
    public SwitchResult switchRepository(
            WorkspaceRepository repository,
            String targetBranch,
            List<String> mainBranchCandidates
    ) {
        return switchRepository(repository, targetBranch, mainBranchCandidates, ChangeProtectionMode.SHELVE);
    }

    /**
     * 对单个仓库执行目标分支切换，并在切换前处理未提交变更。
     *
     * @param repository 仓库快照
     * @param targetBranch 目标分支
     * @param mainBranchCandidates 主分支候选顺序
     * @param changeProtectionMode 变更保护模式
     * @return 单仓库切换结果
     */
    public SwitchResult switchRepository(
            WorkspaceRepository repository,
            String targetBranch,
            List<String> mainBranchCandidates,
            ChangeProtectionMode changeProtectionMode
    ) {
        if (repository.isOperationBlocked()) {
            return buildFailedResult(repository.getId(), targetBranch, repository.getBlockReason());
        }

        Path repositoryRoot = Path.of(repository.getRootPath());
        String currentBranch = resolveCurrentBranch(repositoryRoot);
        if (targetBranch.equals(currentBranch)) {
            repository.setCurrentBranch(currentBranch);
            repository.setLastOperationResultSummary(BUNDLE.getString("result.switch.skipped.current"));
            return buildSkippedResult(repository.getId(), targetBranch, BUNDLE.getString("result.switch.skipped.current"));
        }

        ChangeProtectionService.ChangeProtectionResult protectionResult = protectRepositoryChanges(repository, changeProtectionMode);
        if (protectionResult.isSkipped()) {
            repository.setLastOperationResultSummary(BUNDLE.getString("result.switch.skipped.changes"));
            return buildSkippedResult(repository.getId(), targetBranch, BUNDLE.getString("result.switch.skipped.changes"));
        }
        if (!protectionResult.isClean() && !protectionResult.isProtected()) {
            return buildFailedResult(repository.getId(), targetBranch, protectionResult.message());
        }
        TemporaryChangeRecord temporaryChangeRecord = protectionResult.getRecord();

        GitCommandResult fetchResult = hasOriginRemote(repositoryRoot)
                ? refreshRemoteBranches(repositoryRoot)
                : GitCommandResult.success("", "未配置 origin 远端");
        BranchCheckoutResult targetCheckoutResult = checkoutTargetBranch(repositoryRoot, targetBranch, fetchResult);
        if (targetCheckoutResult.success()) {
            return withTemporaryChangeRecord(
                    applySuccess(repository, targetBranch, targetBranch, targetCheckoutResult.branchSource()),
                    temporaryChangeRecord
            );
        }

        for (String mainBranchCandidate : mainBranchCandidates) {
            BranchCheckoutResult fallbackCheckoutResult = checkoutTargetBranch(repositoryRoot, mainBranchCandidate, fetchResult);
            if (fallbackCheckoutResult.success()) {
                return withTemporaryChangeRecord(
                        applyFallback(repository, targetBranch, mainBranchCandidate, fallbackCheckoutResult.branchSource()),
                        temporaryChangeRecord
                );
            }
            if (fallbackCheckoutResult.hadBranchButCheckoutFailed()) {
                return buildFailedResult(repository.getId(), targetBranch, fallbackCheckoutResult.message());
            }
        }

        repository.setLastOperationResultSummary(BUNDLE.getString("result.switch.skipped"));
        repository.setTargetBranchState(BUNDLE.getString("status.branch.missing"));
        return buildFailedResult(repository.getId(), targetBranch, targetCheckoutResult.message());
    }

    /**
     * 切换前保护仓库未提交变更。
     *
     * @param repository 仓库快照
     * @param changeProtectionMode 变更保护模式
     * @return 保护处理结果
     */
    public ChangeProtectionService.ChangeProtectionResult protectRepositoryChanges(
            WorkspaceRepository repository,
            ChangeProtectionMode changeProtectionMode
    ) {
        ChangeProtectionMode resolvedMode = changeProtectionMode == ChangeProtectionMode.ASK
                ? ChangeProtectionMode.SHELVE
                : changeProtectionMode;
        // 在 IDEA 插件场景下，变更保护应优先且默认只走 IDEA Shelf。
        // Git stash 的实现暂时保留在 ChangeProtectionService 中，作为未来无 Shelf 环境的显式兜底能力；
        // 但当前产品入口不再自动降级到 stash，避免把 IDEA 已忽略或用户未打算处理的本地文件塞进 Git 暂存栈。
        return changeProtectionService.protectChanges(repository, resolvedMode, false);
    }

    /**
     * 刷新仓库的远端引用。
     *
     * @param repositoryRoot 仓库根路径
     * @return Git 命令结果
     */
    public GitCommandResult refreshRemoteBranches(Path repositoryRoot) {
        return runGitCommand(repositoryRoot, "fetch", "--prune", "origin");
    }

    /**
     * 判断远端分支是否存在。
     *
     * @param repositoryRoot 仓库根路径
     * @param branchName 分支名称
     * @return 远端分支存在返回 {@code true}
     */
    public boolean remoteBranchExists(Path repositoryRoot, String branchName) {
        GitCommandResult result = runGitCommand(repositoryRoot, "show-ref", "--verify", "refs/remotes/origin/" + branchName);
        return result.success();
    }

    /**
     * 判断本地分支是否存在。
     *
     * @param repositoryRoot 仓库根路径
     * @param branchName 分支名称
     * @return 本地分支存在返回 {@code true}
     */
    public boolean localBranchExists(Path repositoryRoot, String branchName) {
        GitCommandResult result = runGitCommand(repositoryRoot, "show-ref", "--verify", "refs/heads/" + branchName);
        return result.success();
    }

    private boolean hasOriginRemote(Path repositoryRoot) {
        GitCommandResult result = runGitCommand(repositoryRoot, "remote", "get-url", "origin");
        return result.success();
    }

    private GitCommandResult checkoutRemoteBranch(Path repositoryRoot, String branchName) {
        // 使用真实 Git checkout 创建/重置本地跟踪分支，保证 HEAD 与工作区文件同步。
        return runGitCommand(repositoryRoot, "checkout", "-B", branchName, "origin/" + branchName);
    }

    private GitCommandResult checkoutLocalBranch(Path repositoryRoot, String branchName) {
        return runGitCommand(repositoryRoot, "checkout", branchName);
    }

    private BranchCheckoutResult checkoutTargetBranch(
            Path repositoryRoot,
            String branchName,
            GitCommandResult fetchResult
    ) {
        boolean remoteExists = remoteBranchExists(repositoryRoot, branchName);
        boolean localExists = localBranchExists(repositoryRoot, branchName);
        List<String> failureMessages = new ArrayList<>();

        if (remoteExists && fetchResult.success()) {
            GitCommandResult remoteCheckoutResult = checkoutRemoteBranch(repositoryRoot, branchName);
            if (remoteCheckoutResult.success()) {
                return BranchCheckoutResult.success(BUNDLE.getString("status.branch.remote"));
            }
            failureMessages.add("远端分支切换失败：" + remoteCheckoutResult.stderrOrStdout());
        } else if (remoteExists) {
            failureMessages.add("远端刷新失败，已尝试降级本地分支：" + fetchResult.stderrOrStdout());
        } else if (!fetchResult.success()) {
            failureMessages.add("远端刷新失败，且本地远端引用中未找到目标分支：" + fetchResult.stderrOrStdout());
        }

        if (localExists) {
            GitCommandResult localCheckoutResult = checkoutLocalBranch(repositoryRoot, branchName);
            if (localCheckoutResult.success()) {
                return BranchCheckoutResult.success(BUNDLE.getString("status.branch.local.source"));
            }
            failureMessages.add("本地分支切换失败：" + localCheckoutResult.stderrOrStdout());
        }

        if (failureMessages.isEmpty()) {
            failureMessages.add("远端和本地均未找到分支：" + branchName);
        } else if (!localExists) {
            failureMessages.add("本地分支不存在：" + branchName);
        }
        return BranchCheckoutResult.failed(String.join("；", failureMessages), remoteExists || localExists);
    }

    private GitCommandResult runGitCommand(Path repositoryRoot, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String argument : arguments) {
            command.add(argument);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(repositoryRoot.toFile());
        processBuilder.redirectErrorStream(false);
        try {
            Process process = processBuilder.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            int exitCode = process.waitFor();
            return new GitCommandResult(exitCode == 0, stdout.trim(), stderr.trim(), exitCode);
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new GitCommandResult(false, "", "执行 Git 命令失败：" + exception.getMessage(), -1);
        }
    }

    private String readStream(java.io.InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!builder.isEmpty()) {
                    builder.append(System.lineSeparator());
                }
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private SwitchResult applySuccess(
            WorkspaceRepository repository,
            String requestedBranch,
            String finalBranch,
            String branchSource
    ) {
        repository.setCurrentBranch(resolveCurrentBranch(Path.of(repository.getRootPath())));
        repository.setTargetBranchState(BUNDLE.getString("status.remote.branch").equals(branchSource)
                ? BUNDLE.getString("status.remote.branch")
                : BUNDLE.getString("status.branch.local"));
        repository.setLastOperationResultSummary(BUNDLE.getString("result.switch.success"));
        return buildSuccessResult(repository.getId(), requestedBranch, finalBranch, false, branchSource);
    }

    private SwitchResult applyFallback(
            WorkspaceRepository repository,
            String requestedBranch,
            String finalBranch,
            String branchSource
    ) {
        repository.setCurrentBranch(resolveCurrentBranch(Path.of(repository.getRootPath())));
        repository.setTargetBranchState(BUNDLE.getString("status.fallback"));
        repository.setLastOperationResultSummary(BUNDLE.getString("result.switch.fallback"));
        return buildFallbackResult(repository.getId(), requestedBranch, finalBranch, branchSource);
    }

    private String resolveCurrentBranch(Path repositoryRoot) {
        GitCommandResult result = runGitCommand(repositoryRoot, "rev-parse", "--abbrev-ref", "HEAD");
        return result.success() && !result.stdout().isBlank()
                ? result.stdout()
                : BUNDLE.getString("status.branch.unknown");
    }

    private SwitchResult buildSuccessResult(
            String repositoryId,
            String requestedBranch,
            String finalBranch,
            boolean fallbackUsed,
            String branchSource
    ) {
        return new SwitchResult(
                repositoryId,
                SwitchResultStatus.SUCCESS,
                requestedBranch,
                finalBranch,
                fallbackUsed,
                "",
                BUNDLE.getString("result.switch.success"),
                "",
                Instant.now(),
                branchSource
        );
    }

    private SwitchResult buildFallbackResult(String repositoryId, String requestedBranch, String finalBranch, String branchSource) {
        return new SwitchResult(
                repositoryId,
                SwitchResultStatus.FALLBACK_TO_MAIN,
                requestedBranch,
                finalBranch,
                true,
                "",
                BUNDLE.getString("result.switch.fallback"),
                "",
                Instant.now(),
                branchSource
        );
    }

    private SwitchResult buildSkippedResult(String repositoryId, String requestedBranch) {
        return buildSkippedResult(repositoryId, requestedBranch, BUNDLE.getString("result.switch.skipped"));
    }

    private SwitchResult buildSkippedResult(String repositoryId, String requestedBranch, String message) {
        return new SwitchResult(
                repositoryId,
                SwitchResultStatus.SKIPPED,
                requestedBranch,
                "",
                false,
                "",
                message,
                "",
                Instant.now(),
                ""
        );
    }

    private SwitchResult buildFailedResult(String repositoryId, String requestedBranch, String failureReason) {
        return new SwitchResult(
                repositoryId,
                SwitchResultStatus.FAILED,
                requestedBranch,
                "",
                false,
                "",
                BUNDLE.getString("result.switch.failed"),
                failureReason,
                Instant.now(),
                ""
        );
    }

    private SwitchResult withTemporaryChangeRecord(SwitchResult result, TemporaryChangeRecord temporaryChangeRecord) {
        if (temporaryChangeRecord == null) {
            return result;
        }
        return new SwitchResult(
                result.getRepositoryId(),
                SwitchResultStatus.MANUAL_RESTORE_REQUIRED,
                result.getRequestedBranch(),
                result.getFinalBranch(),
                result.isFallbackUsed(),
                temporaryChangeRecord.getRecordId(),
                BUNDLE.getString("result.switch.restore.required") + "：" + temporaryChangeRecord.getRestoreHint(),
                result.getFailureReason(),
                result.getFinishedAt(),
                result.getBranchSource()
        );
    }

    /**
     * 表示一次 Git 命令执行结果。
     *
     * @param success 是否成功
     * @param stdout 标准输出
     * @param stderr 标准错误
     * @param exitCode 退出码
     * @author August Lee
     */
    public record GitCommandResult(boolean success, String stdout, String stderr, int exitCode) {

        /**
         * 创建成功的 Git 命令结果。
         *
         * @param stdout 标准输出
         * @param stderr 标准错误
         * @return 成功结果
         */
        public static GitCommandResult success(String stdout, String stderr) {
            return new GitCommandResult(true, stdout == null ? "" : stdout, stderr == null ? "" : stderr, 0);
        }

        /**
         * 返回更适合向上层展示的错误或输出文本。
         *
         * @return 错误输出优先，其次返回标准输出
         */
        public String stderrOrStdout() {
            if (stderr != null && !stderr.isBlank()) {
                return stderr;
            }
            return stdout == null || stdout.isBlank() ? "Git 命令执行失败" : stdout;
        }
    }

    /**
     * 表示目标分支或回退分支的切换结果。
     *
     * @param success 是否切换成功
     * @param branchSource 成功切换时的分支来源
     * @param message 失败原因
     * @param hadBranchButCheckoutFailed 是否曾找到分支但切换失败
     * @author August Lee
     */
    private record BranchCheckoutResult(
            boolean success,
            String branchSource,
            String message,
            boolean hadBranchButCheckoutFailed
    ) {

        /**
         * 创建成功结果。
         *
         * @param branchSource 分支来源
         * @return 成功结果
         */
        private static BranchCheckoutResult success(String branchSource) {
            return new BranchCheckoutResult(true, branchSource, "", false);
        }

        /**
         * 创建失败结果。
         *
         * @param message 失败原因
         * @param hadBranchButCheckoutFailed 是否曾找到分支但切换失败
         * @return 失败结果
         */
        private static BranchCheckoutResult failed(String message, boolean hadBranchButCheckoutFailed) {
            return new BranchCheckoutResult(false, "", message, hadBranchButCheckoutFailed);
        }
    }
}
