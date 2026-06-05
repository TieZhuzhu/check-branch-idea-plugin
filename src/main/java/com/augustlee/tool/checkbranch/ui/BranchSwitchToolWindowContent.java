package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.BranchSwitchRequest;
import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
import com.augustlee.tool.checkbranch.model.SwitchResult;
import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import com.augustlee.tool.checkbranch.notification.CheckBranchNotifier;
import com.augustlee.tool.checkbranch.service.BranchSwitchService;
import com.augustlee.tool.checkbranch.service.BranchPreferenceService;
import com.augustlee.tool.checkbranch.service.ChangeProtectionService;
import com.augustlee.tool.checkbranch.service.IdeaShelveExecutor;
import com.augustlee.tool.checkbranch.service.RepositoryDiscoveryService;
import com.augustlee.tool.checkbranch.service.SwitchResultStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 右侧工具窗口的主内容面板骨架。
 *
 * @author August Lee
 */
public class BranchSwitchToolWindowContent extends JPanel {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages.CheckBranchBundle");
    private static final DateTimeFormatter REFRESH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Project project;
    private final RepositoryDiscoveryService repositoryDiscoveryService = new RepositoryDiscoveryService();
    private final BranchSwitchService branchSwitchService;
    private final CheckBranchNotifier checkBranchNotifier = new CheckBranchNotifier();
    private final RepositoryTableModel repositoryTableModel = new RepositoryTableModel();
    private final JTable repositoryTable = new JBTable(repositoryTableModel);
    private final SwitchResultPanel switchResultPanel = new SwitchResultPanel();
    private final JBTextField targetBranchField = new JBTextField();
    private final JButton refreshButton = new JButton(BUNDLE.getString("action.refresh"));
    private final JButton switchButton = new JButton(BUNDLE.getString("action.switch"));
    private final JBCheckBox selectAllCheckBox = new JBCheckBox(BUNDLE.getString("action.select.all"));
    private final JBLabel selectionCountLabel = new JBLabel(BUNDLE.getString("label.selection.count"));
    private final JBLabel lastRefreshTimeLabel = new JBLabel(BUNDLE.getString("label.refresh.time") + "：未刷新");
    private final JBLabel statusLabel = new JBLabel(BUNDLE.getString("status.pending"));
    private final JProgressBar operationProgressBar = new JProgressBar();

    /**
     * 创建工具窗口主面板。
     *
     * @param project 当前 IDEA 项目
     */
    public BranchSwitchToolWindowContent(Project project) {
        super(new BorderLayout(0, 8));
        this.project = project;
        this.branchSwitchService = new BranchSwitchService(new ChangeProtectionService(new IdeaShelveExecutor(project)));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(switchResultPanel, BorderLayout.SOUTH);
        configureRepositoryTable();
        installListeners();
        refreshRepositoryStatus(false);
        loadLastResults();
    }

    /**
     * 返回当前项目实例。
     *
     * @return 当前 IDEA 项目
     */
    public Project getProject() {
        return project;
    }

    /**
     * 返回仓库表格模型。
     *
     * @return 仓库表格模型
     */
    public RepositoryTableModel getRepositoryTableModel() {
        return repositoryTableModel;
    }

    /**
     * 返回结果面板。
     *
     * @return 结果面板
     */
    public SwitchResultPanel getSwitchResultPanel() {
        return switchResultPanel;
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        JBLabel targetBranchLabel = new JBLabel(BUNDLE.getString("label.target.branch"));
        inputRow.add(targetBranchLabel, BorderLayout.WEST);
        inputRow.add(targetBranchField, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        actionPanel.add(refreshButton);
        actionPanel.add(switchButton);
        inputRow.add(actionPanel, BorderLayout.EAST);

        JPanel statusRow = new JPanel();
        statusRow.setLayout(new BoxLayout(statusRow, BoxLayout.X_AXIS));
        statusRow.add(selectionCountLabel);
        statusRow.add(Box.createHorizontalStrut(16));
        statusRow.add(lastRefreshTimeLabel);
        statusRow.add(Box.createHorizontalStrut(16));
        statusRow.add(statusLabel);
        operationProgressBar.setIndeterminate(true);
        operationProgressBar.setVisible(false);
        operationProgressBar.setPreferredSize(new Dimension(120, 14));
        statusRow.add(Box.createHorizontalStrut(12));
        statusRow.add(operationProgressBar);

        topPanel.add(inputRow);
        topPanel.add(statusRow);
        return topPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        JBLabel repositoryListLabel = new JBLabel(BUNDLE.getString("label.repository.list"));
        repositoryListLabel.setAlignmentX(LEFT_ALIGNMENT);
        repositoryListLabel.setHorizontalAlignment(SwingConstants.LEFT);
        repositoryListLabel.setFont(repositoryListLabel.getFont().deriveFont(repositoryListLabel.getFont().getSize2D() + 1.0f));
        JPanel selectionRow = createSelectionRow();
        JScrollPane tableScrollPane = new JScrollPane(repositoryTable);
        tableScrollPane.setPreferredSize(new Dimension(520, 260));
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(repositoryListLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(selectionRow);
        centerPanel.add(headerPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);
        return centerPanel;
    }

    private JPanel createSelectionRow() {
        JPanel selectionRow = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.add(selectAllCheckBox);
        selectionRow.add(leftPanel, BorderLayout.WEST);
        return selectionRow;
    }

    private void configureRepositoryTable() {
        repositoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repositoryTable.setRowHeight(30);
        repositoryTable.getAccessibleContext().setAccessibleName("工作区仓库列表");
        targetBranchField.getAccessibleContext().setAccessibleName("目标分支输入框");
        refreshButton.getAccessibleContext().setAccessibleName("刷新仓库状态按钮");
        selectAllCheckBox.getAccessibleContext().setAccessibleName("全选仓库勾选框");
        switchButton.getAccessibleContext().setAccessibleName("执行分支切换按钮");
        repositoryTable.getColumnModel().getColumn(0).setMaxWidth(70);
        repositoryTable.getColumnModel().getColumn(0).setMinWidth(70);
        repositoryTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        repositoryTable.getColumnModel().getColumn(1).setMaxWidth(160);
        repositoryTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        repositoryTable.getColumnModel().getColumn(2).setMaxWidth(250);
        repositoryTable.getColumnModel().getColumn(3).setMaxWidth(120);
        repositoryTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        configureTableHeaderRenderer();
    }

    private void installListeners() {
        repositoryTableModel.addTableModelListener(event -> refreshSelectionCount());
        selectAllCheckBox.addActionListener(event -> repositoryTableModel.setAllSelected(selectAllCheckBox.isSelected()));
        refreshButton.addActionListener(event -> refreshRepositoryStatus(true));
        switchButton.addActionListener(event -> executeBatchSwitch());
    }

    private void refreshRepositoryStatus(boolean background) {
        if (!background) {
            refreshRepositoryStatusInCurrentThread();
            return;
        }
        String targetBranch = targetBranchField.getText() == null ? "" : targetBranchField.getText().trim();
        List<String> mainBranchCandidates = BranchPreferenceService.getInstance(project).getMainBranchCandidates();
        statusLabel.setText(BUNDLE.getString("status.loading"));
        refreshButton.setEnabled(false);
        switchButton.setEnabled(false);
        operationProgressBar.setVisible(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "刷新仓库状态", false) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("正在读取 IDEA Git 仓库状态...");
                try {
                    List<WorkspaceRepository> repositories = repositoryDiscoveryService.discoverRepositories(project);
                    previewTargetBranchState(repositories, targetBranch, mainBranchCandidates);
                    ApplicationManager.getApplication().invokeLater(() -> applyRepositoryRefreshSuccess(repositories));
                } catch (RuntimeException exception) {
                    ApplicationManager.getApplication().invokeLater(() -> applyRepositoryRefreshFailure());
                }
            }
        });
    }

    private void refreshRepositoryStatusInCurrentThread() {
        statusLabel.setText(BUNDLE.getString("status.loading"));
        refreshButton.setEnabled(false);
        switchButton.setEnabled(false);
        operationProgressBar.setVisible(true);
        try {
            List<WorkspaceRepository> repositories = repositoryDiscoveryService.discoverRepositories(project);
            previewTargetBranchState(
                    repositories,
                    targetBranchField.getText() == null ? "" : targetBranchField.getText().trim(),
                    BranchPreferenceService.getInstance(project).getMainBranchCandidates()
            );
            applyRepositoryRefreshSuccess(repositories);
        } catch (RuntimeException exception) {
            applyRepositoryRefreshFailure();
        } finally {
            refreshButton.setEnabled(true);
            switchButton.setEnabled(true);
            operationProgressBar.setVisible(false);
        }
    }

    private void applyRepositoryRefreshSuccess(List<WorkspaceRepository> repositories) {
        repositoryTableModel.setRepositories(repositories);
        if (!repositoryTableModel.isEmpty()) {
            repositoryTableModel.setAllSelected(true);
            selectAllCheckBox.setSelected(true);
        } else {
            selectAllCheckBox.setSelected(false);
        }
        switchResultPanel.showEmptyState(BUNDLE.getString("label.result.empty"));
        refreshSelectionCount();
        updateRefreshTime();
        statusLabel.setText(repositoryTableModel.isEmpty()
                ? BUNDLE.getString("label.repository.unavailable")
                : BUNDLE.getString("status.refresh.success"));
        refreshButton.setEnabled(true);
        switchButton.setEnabled(true);
        operationProgressBar.setVisible(false);
    }

    private void applyRepositoryRefreshFailure() {
        repositoryTableModel.setRepositories(List.of());
        selectAllCheckBox.setSelected(false);
        refreshSelectionCount();
        lastRefreshTimeLabel.setText(BUNDLE.getString("label.refresh.time") + "：未刷新");
        statusLabel.setText(BUNDLE.getString("status.refresh.failed"));
        switchResultPanel.showEmptyState(BUNDLE.getString("status.refresh.failed"));
        checkBranchNotifier.notifyError(
                project,
                BUNDLE.getString("notification.title.error"),
                BUNDLE.getString("notification.refresh.failed.content")
        );
        refreshButton.setEnabled(true);
        switchButton.setEnabled(true);
        operationProgressBar.setVisible(false);
    }

    private void refreshSelectionCount() {
        int selectedCount = repositoryTableModel.getSelectedRepositories().size();
        selectionCountLabel.setText("已选择 " + selectedCount + " 个仓库");
        if (!repositoryTableModel.isEmpty()) {
            selectAllCheckBox.setSelected(repositoryTableModel.isAllSelected());
        }
    }

    private void updateRefreshTime() {
        String refreshAt = BranchPreferenceService.getInstance(project).getLastRefreshAt();
        if (refreshAt == null || refreshAt.isBlank()) {
            lastRefreshTimeLabel.setText(BUNDLE.getString("label.refresh.time") + "：未刷新");
            return;
        }
        lastRefreshTimeLabel.setText(
                BUNDLE.getString("label.refresh.time") + "：" + REFRESH_TIME_FORMATTER.format(Instant.parse(refreshAt))
        );
    }

    private void executeBatchSwitch() {
        String targetBranch = targetBranchField.getText() == null ? "" : targetBranchField.getText().trim();
        targetBranchField.setText(targetBranch);
        BranchPreferenceService branchPreferenceService = BranchPreferenceService.getInstance(project);
        if (targetBranch.isEmpty()) {
            statusLabel.setText(BUNDLE.getString("validation.branch.empty"));
            return;
        }
        if (!branchPreferenceService.isValidTargetBranch(targetBranch)) {
            statusLabel.setText(BUNDLE.getString("validation.branch.invalid"));
            return;
        }
        List<WorkspaceRepository> selectedRepositories = repositoryTableModel.getSelectedRepositories();
        if (selectedRepositories.isEmpty()) {
            statusLabel.setText(BUNDLE.getString("validation.repository.empty"));
            return;
        }
        Map<String, ChangeProtectionMode> changeProtectionModes = collectChangeProtectionModes(selectedRepositories);
        if (changeProtectionModes == null) {
            statusLabel.setText(BUNDLE.getString("dialog.unsaved.cancel"));
            return;
        }

        List<String> repositoryIds = selectedRepositories.stream()
                .map(WorkspaceRepository::getId)
                .toList();
        BranchSwitchRequest request = new BranchSwitchRequest(
                "request-" + Instant.now().toEpochMilli(),
                targetBranch,
                repositoryIds,
                ChangeProtectionMode.ASK,
                Instant.now(),
                false
        );

        // 分支切换会触发真实 Git checkout/fetch，必须放到后台线程，避免阻塞 IDEA 主线程。
        List<WorkspaceRepository> repositorySnapshot = new ArrayList<>(repositoryTableModel.getRepositories());
        List<String> mainBranchCandidates = branchPreferenceService.getMainBranchCandidates();
        switchButton.setEnabled(false);
        refreshButton.setEnabled(false);
        operationProgressBar.setVisible(true);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "执行分支切换", false) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("正在执行分支切换...");
                try {
                    List<SwitchResult> results = branchSwitchService.switchBranches(
                            request,
                            repositorySnapshot,
                            mainBranchCandidates,
                            changeProtectionModes
                    );
                    ApplicationManager.getApplication().invokeLater(() -> {
                        applySwitchResults(branchPreferenceService, targetBranch, results);
                    });
                } catch (RuntimeException exception) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        statusLabel.setText("分支切换执行异常：" + exception.getMessage());
                        checkBranchNotifier.notifyError(
                                project,
                                BUNDLE.getString("notification.title.error"),
                                "分支切换执行异常，请查看 IDEA 日志或结果区。"
                        );
                        switchButton.setEnabled(true);
                        refreshButton.setEnabled(true);
                        operationProgressBar.setVisible(false);
                    });
                }
            }
        });
    }

    private void applySwitchResults(
            BranchPreferenceService branchPreferenceService,
            String targetBranch,
            List<SwitchResult> results
    ) {
        branchPreferenceService.rememberTargetBranch(targetBranch);
        SwitchResultStateService.getInstance(project).saveResults(results);
        switchResultPanel.showResults(results);
        for (SwitchResult result : results) {
            repositoryTableModel.updateLastResultSummary(result.getRepositoryId(), result.toSummaryText());
        }
        boolean containsWarningResult = results.stream().anyMatch(result ->
                result.getStatus() == com.augustlee.tool.checkbranch.model.SwitchResultStatus.SKIPPED
                        || result.getStatus() == com.augustlee.tool.checkbranch.model.SwitchResultStatus.FAILED
                        || result.getStatus() == com.augustlee.tool.checkbranch.model.SwitchResultStatus.MANUAL_RESTORE_REQUIRED
        );
        boolean singleRepository = results.size() == 1;
        String successMessage = singleRepository
                ? BUNDLE.getString("notification.switch.single.finished.content")
                : BUNDLE.getString("notification.switch.finished.content");
        statusLabel.setText(successMessage);
        if (containsWarningResult) {
            checkBranchNotifier.notifyWarning(
                    project,
                    BUNDLE.getString("notification.title.warning"),
                    BUNDLE.getString("notification.switch.warning.content")
            );
        } else {
            checkBranchNotifier.notifyInfo(
                    project,
                    BUNDLE.getString("notification.title.info"),
                    successMessage
            );
        }
        switchButton.setEnabled(true);
        refreshButton.setEnabled(true);
        operationProgressBar.setVisible(false);
    }

    private Map<String, ChangeProtectionMode> collectChangeProtectionModes(List<WorkspaceRepository> selectedRepositories) {
        Map<String, ChangeProtectionMode> changeProtectionModes = new HashMap<>();
        for (WorkspaceRepository repository : selectedRepositories) {
            if (!repository.hasUncommittedChanges()) {
                continue;
            }
            ChangeProtectionMode selectedMode = askChangeProtectionMode(repository);
            if (selectedMode == null) {
                return null;
            }
            changeProtectionModes.put(repository.getId(), selectedMode);
        }
        return changeProtectionModes;
    }

    private ChangeProtectionMode askChangeProtectionMode(WorkspaceRepository repository) {
        String[] options = {
                BUNDLE.getString("dialog.unsaved.shelve"),
                // Git stash 不再作为默认入口暴露：在 IDEA 插件内优先使用 Shelf，保持与用户在 IDEA 中手动切分支的一致体验。
                // 如后续确认需要为无 Shelf 环境提供兜底，再恢复该选项并同步恢复服务层降级开关。
                // BUNDLE.getString("dialog.unsaved.stash"),
                BUNDLE.getString("dialog.unsaved.skip"),
                BUNDLE.getString("dialog.unsaved.cancel")
        };
        int selectedOption = Messages.showDialog(
                project,
                java.text.MessageFormat.format(BUNDLE.getString("dialog.unsaved.detail"), repository.getDisplayName()),
                BUNDLE.getString("dialog.unsaved.title"),
                options,
                1,
                Messages.getWarningIcon()
        );
        if (selectedOption == 0) {
            return ChangeProtectionMode.SHELVE;
        }
        if (selectedOption == 1) {
            return ChangeProtectionMode.SKIP;
        }
        return null;
    }

    private final class StatusCellRenderer extends DefaultTableCellRenderer {

        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setOpaque(true);
            RepositoryTableModel.RepositoryStatusColor statusColor = repositoryTableModel.getStatusColorAt(row);
            if (!isSelected) {
                switch (statusColor) {
                    case SUCCESS -> {
                        label.setBackground(new Color(232, 245, 233));
                        label.setForeground(new Color(27, 94, 32));
                    }
                    case WARNING -> {
                        label.setBackground(new Color(255, 243, 224));
                        label.setForeground(new Color(230, 81, 0));
                    }
                    case DANGER -> {
                        label.setBackground(new Color(255, 235, 238));
                        label.setForeground(new Color(183, 28, 28));
                    }
                    default -> {
                        label.setBackground(table.getBackground());
                        label.setForeground(table.getForeground());
                    }
                }
            }
            return label;
        }
    }

    private void loadLastResults() {
        List<SwitchResult> lastResults = SwitchResultStateService.getInstance(project).getLastResults();
        if (!lastResults.isEmpty()) {
            switchResultPanel.showResults(lastResults);
        }
    }

    /**
     * JBTable 的表头默认 renderer 并不保证是 {@link DefaultTableCellRenderer}，这里用包装器统一左对齐，
     * 避免在不同 IDE 版本下出现类型转换异常，导致整个工具窗口内容初始化失败。
     */
    private void configureTableHeaderRenderer() {
        TableCellRenderer originalRenderer = repositoryTable.getTableHeader().getDefaultRenderer();
        repositoryTable.getTableHeader().setDefaultRenderer((table, value, isSelected, hasFocus, row, column) -> {
            java.awt.Component component = originalRenderer.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return component;
        });
    }

    private void previewTargetBranchState(
            List<WorkspaceRepository> repositories,
            String targetBranch,
            List<String> mainBranchCandidates
    ) {
        for (WorkspaceRepository repository : repositories) {
            if (targetBranch.isBlank()) {
                repository.setTargetBranchState(BUNDLE.getString("status.branch.unknown"));
                continue;
            }
            repository.setTargetBranchState(resolveTargetBranchState(repository, targetBranch, mainBranchCandidates));
        }
    }

    private String resolveTargetBranchState(WorkspaceRepository repository, String targetBranch, List<String> mainBranchCandidates) {
        Path repositoryRoot = Path.of(repository.getRootPath());
        if (targetBranch.equals(repository.getCurrentBranch())) {
            return BUNDLE.getString("result.switch.skipped.current");
        }
        if (branchSwitchService.remoteBranchExists(repositoryRoot, targetBranch)) {
            return BUNDLE.getString("status.remote.branch");
        }
        if (branchSwitchService.localBranchExists(repositoryRoot, targetBranch)) {
            return BUNDLE.getString("status.branch.local");
        }
        for (String mainBranchCandidate : mainBranchCandidates) {
            if (branchSwitchService.remoteBranchExists(repositoryRoot, mainBranchCandidate)
                    || branchSwitchService.localBranchExists(repositoryRoot, mainBranchCandidate)) {
                return BUNDLE.getString("status.fallback") + " " + mainBranchCandidate;
            }
        }
        return BUNDLE.getString("status.branch.missing");
    }
}
