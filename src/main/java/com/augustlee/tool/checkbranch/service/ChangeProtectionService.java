package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
import com.augustlee.tool.checkbranch.model.TemporaryChangeRecord;
import com.augustlee.tool.checkbranch.model.WorkspaceRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * 在切换分支前保护仓库中的未提交变更。
 *
 * <p>首版优先尝试 IDEA 搁置变更路径，默认执行器会报告当前后台 Git CLI 环境不可用；
 * 当调用方选择允许降级时，本服务使用真实 {@code git stash push -u} 保存变更。</p>
 *
 * @author August Lee
 */
public class ChangeProtectionService {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages.CheckBranchBundle");

    private final ShelveExecutor shelveExecutor;

    /**
     * 使用默认搁置执行器创建变更保护服务。
     */
    public ChangeProtectionService() {
        this(repository -> ProtectionCommandResult.failed(BUNDLE.getString("protection.shelve.unavailable")));
    }

    /**
     * 使用指定搁置执行器创建变更保护服务，便于测试或后续接入 IDEA 原生搁置 API。
     *
     * @param shelveExecutor 搁置变更执行器
     */
    public ChangeProtectionService(ShelveExecutor shelveExecutor) {
        this.shelveExecutor = Objects.requireNonNull(shelveExecutor, "搁置执行器不能为空");
    }

    /**
     * 按指定模式保护仓库未提交变更。
     *
     * @param repository 当前仓库
     * @param requestedMode 用户选择的保护模式
     * @param allowStashFallback 搁置失败时是否允许降级到 Git 暂存栈
     * @return 保护处理结果
     */
    public ChangeProtectionResult protectChanges(
            WorkspaceRepository repository,
            ChangeProtectionMode requestedMode,
            boolean allowStashFallback
    ) {
        if (repository == null) {
            return ChangeProtectionResult.failed("仓库不能为空");
        }
        if (!hasUncommittedChanges(Path.of(repository.getRootPath()))) {
            repository.setHasUncommittedChanges(false);
            return ChangeProtectionResult.clean();
        }

        ChangeProtectionMode mode = requestedMode == null ? ChangeProtectionMode.SHELVE : requestedMode;
        if (mode == ChangeProtectionMode.SKIP) {
            return ChangeProtectionResult.skipped(BUNDLE.getString("protection.skipped"));
        }
        if (mode == ChangeProtectionMode.GIT_STASH) {
            return stashChanges(repository);
        }

        ProtectionCommandResult shelveResult = shelveExecutor.shelve(repository);
        if (shelveResult.success()) {
            repository.setHasUncommittedChanges(false);
            return ChangeProtectionResult.protectedBy(createRecord(
                    repository,
                    ChangeProtectionMode.SHELVE,
                    BUNDLE.getString("protection.shelve.label"),
                    BUNDLE.getString("protection.shelve.restore.hint")
            ));
        }
        if (!allowStashFallback) {
            return ChangeProtectionResult.failed(shelveResult.message());
        }

        ChangeProtectionResult stashResult = stashChanges(repository);
        if (stashResult.isProtected()) {
            return stashResult;
        }
        return ChangeProtectionResult.failed(shelveResult.message() + "；" + stashResult.message());
    }

    /**
     * 判断仓库是否存在未提交变更。
     *
     * @param repositoryRoot 仓库根路径
     * @return 存在未提交变更返回 {@code true}
     */
    public boolean hasUncommittedChanges(Path repositoryRoot) {
        ProtectionCommandResult result = runGitCommand(repositoryRoot, "status", "--porcelain", "--untracked-files=no");
        return result.success() && !result.message().isBlank();
    }

    private ChangeProtectionResult stashChanges(WorkspaceRepository repository) {
        String stashLabel = BUNDLE.getString("protection.stash.label") + " " + DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        ProtectionCommandResult stashResult = runGitCommand(
                Path.of(repository.getRootPath()),
                "stash",
                "push",
                "-u",
                "-m",
                stashLabel
        );
        if (!stashResult.success()) {
            return ChangeProtectionResult.failed(stashResult.message().isBlank()
                    ? BUNDLE.getString("protection.stash.failed")
                    : stashResult.message());
        }
        repository.setHasUncommittedChanges(false);
        return ChangeProtectionResult.protectedBy(createRecord(
                repository,
                ChangeProtectionMode.GIT_STASH,
                stashLabel,
                BUNDLE.getString("protection.stash.restore.hint")
        ));
    }

    private TemporaryChangeRecord createRecord(
            WorkspaceRepository repository,
            ChangeProtectionMode method,
            String label,
            String restoreHint
    ) {
        return new TemporaryChangeRecord(
                UUID.randomUUID().toString(),
                repository.getId(),
                method,
                label,
                Instant.now(),
                restoreHint,
                true
        );
    }

    private ProtectionCommandResult runGitCommand(Path repositoryRoot, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(repositoryRoot.toFile());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = readStream(process.getInputStream());
            int exitCode = process.waitFor();
            return exitCode == 0
                    ? ProtectionCommandResult.success(output.trim())
                    : ProtectionCommandResult.failed(output.trim());
        } catch (IOException exception) {
            return ProtectionCommandResult.failed("执行 Git 命令失败：" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ProtectionCommandResult.failed("执行 Git 命令被中断");
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

    /**
     * 执行 IDEA 搁置变更的适配接口。
     *
     * @author August Lee
     */
    @FunctionalInterface
    public interface ShelveExecutor {

        /**
         * 搁置指定仓库的未提交变更。
         *
         * @param repository 当前仓库
         * @return 执行结果
         */
        ProtectionCommandResult shelve(WorkspaceRepository repository);
    }

    /**
     * 表示底层保护命令的执行结果。
     *
     * @param success 是否成功
     * @param message 执行消息
     * @author August Lee
     */
    public record ProtectionCommandResult(boolean success, String message) {

        /**
         * 创建成功结果。
         *
         * @param message 执行消息
         * @return 成功结果
         */
        public static ProtectionCommandResult success(String message) {
            return new ProtectionCommandResult(true, message == null ? "" : message);
        }

        /**
         * 创建失败结果。
         *
         * @param message 失败消息
         * @return 失败结果
         */
        public static ProtectionCommandResult failed(String message) {
            return new ProtectionCommandResult(false, message == null ? "" : message);
        }
    }

    /**
     * 表示一次未提交变更保护流程的结果。
     *
     * @author August Lee
     */
    public static class ChangeProtectionResult {

        private final boolean protectedChanges;
        private final boolean skipped;
        private final boolean clean;
        private final TemporaryChangeRecord record;
        private final String message;

        private ChangeProtectionResult(
                boolean protectedChanges,
                boolean skipped,
                boolean clean,
                TemporaryChangeRecord record,
                String message
        ) {
            this.protectedChanges = protectedChanges;
            this.skipped = skipped;
            this.clean = clean;
            this.record = record;
            this.message = message == null ? "" : message.trim();
        }

        /**
         * 创建工作区干净结果。
         *
         * @return 工作区干净结果
         */
        public static ChangeProtectionResult clean() {
            return new ChangeProtectionResult(false, false, true, null, "");
        }

        /**
         * 创建已保护结果。
         *
         * @param record 临时变更记录
         * @return 已保护结果
         */
        public static ChangeProtectionResult protectedBy(TemporaryChangeRecord record) {
            return new ChangeProtectionResult(true, false, false, record, "");
        }

        /**
         * 创建跳过结果。
         *
         * @param message 跳过原因
         * @return 跳过结果
         */
        public static ChangeProtectionResult skipped(String message) {
            return new ChangeProtectionResult(false, true, false, null, message);
        }

        /**
         * 创建失败结果。
         *
         * @param message 失败原因
         * @return 失败结果
         */
        public static ChangeProtectionResult failed(String message) {
            return new ChangeProtectionResult(false, false, false, null, message);
        }

        /**
         * 返回是否已经临时保存变更。
         *
         * @return 已保存变更返回 {@code true}
         */
        public boolean isProtected() {
            return protectedChanges;
        }

        /**
         * 返回是否跳过当前仓库。
         *
         * @return 跳过返回 {@code true}
         */
        public boolean isSkipped() {
            return skipped;
        }

        /**
         * 返回仓库是否无需保护。
         *
         * @return 无需保护返回 {@code true}
         */
        public boolean isClean() {
            return clean;
        }

        /**
         * 返回关联的临时变更记录。
         *
         * @return 临时变更记录，可能为空
         */
        public TemporaryChangeRecord getRecord() {
            return record;
        }

        /**
         * 返回结果消息。
         *
         * @return 结果消息
         */
        public String message() {
            return message;
        }
    }
}
