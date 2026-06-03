package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import com.augustlee.tool.checkbranch.notification.CheckBranchNotifier;
import com.augustlee.tool.checkbranch.service.BranchPreferenceService;
import com.augustlee.tool.checkbranch.service.RepositoryDiscoveryService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final CheckBranchNotifier checkBranchNotifier = new CheckBranchNotifier();
    private final RepositoryTableModel repositoryTableModel = new RepositoryTableModel();
    private final JTable repositoryTable = new JTable(repositoryTableModel);
    private final SwitchResultPanel switchResultPanel = new SwitchResultPanel();
    private final JBTextField targetBranchField = new JBTextField();
    private final JButton refreshButton = new JButton(BUNDLE.getString("action.refresh"));
    private final JButton switchButton = new JButton(BUNDLE.getString("action.switch"));
    private final JBLabel selectionCountLabel = new JBLabel(BUNDLE.getString("label.selection.count"));
    private final JBLabel lastRefreshTimeLabel = new JBLabel(BUNDLE.getString("label.refresh.time") + "：未刷新");
    private final JBLabel statusLabel = new JBLabel(BUNDLE.getString("status.pending"));

    /**
     * 创建工具窗口主面板。
     *
     * @param project 当前 IDEA 项目
     */
    public BranchSwitchToolWindowContent(Project project) {
        super(new BorderLayout(0, 8));
        this.project = project;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(switchResultPanel, BorderLayout.SOUTH);
        configureRepositoryTable();
        installListeners();
        refreshRepositoryStatus();
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

        topPanel.add(inputRow);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(statusRow);
        return topPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        JScrollPane tableScrollPane = new JScrollPane(repositoryTable);
        tableScrollPane.setPreferredSize(new Dimension(780, 260));
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);
        return centerPanel;
    }

    private void configureRepositoryTable() {
        repositoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repositoryTable.getAccessibleContext().setAccessibleName("工作区仓库列表");
        targetBranchField.getAccessibleContext().setAccessibleName("目标分支输入框");
        refreshButton.getAccessibleContext().setAccessibleName("刷新仓库状态按钮");
        switchButton.getAccessibleContext().setAccessibleName("执行分支切换按钮");
    }

    private void installListeners() {
        repositoryTableModel.addTableModelListener(event -> refreshSelectionCount());
        refreshButton.addActionListener(event -> refreshRepositoryStatus());
        switchButton.addActionListener(event -> statusLabel.setText(BUNDLE.getString("status.expired")));
    }

    private void refreshRepositoryStatus() {
        statusLabel.setText(BUNDLE.getString("status.loading"));
        refreshButton.setEnabled(false);
        try {
            List<WorkspaceRepository> repositories = repositoryDiscoveryService.discoverRepositories(project);
            repositoryTableModel.setRepositories(repositories);
            switchResultPanel.showEmptyState(BUNDLE.getString("label.result.empty"));
            refreshSelectionCount();
            updateRefreshTime();
            if (repositoryTableModel.isEmpty()) {
                statusLabel.setText(BUNDLE.getString("label.repository.unavailable"));
            } else {
                statusLabel.setText(BUNDLE.getString("status.refresh.success"));
            }
        } catch (RuntimeException exception) {
            repositoryTableModel.setRepositories(List.of());
            refreshSelectionCount();
            lastRefreshTimeLabel.setText(BUNDLE.getString("label.refresh.time") + "：未刷新");
            statusLabel.setText(BUNDLE.getString("status.refresh.failed"));
            switchResultPanel.showEmptyState(BUNDLE.getString("status.refresh.failed"));
            checkBranchNotifier.notifyError(
                    project,
                    BUNDLE.getString("notification.title.error"),
                    BUNDLE.getString("notification.refresh.failed.content")
            );
        } finally {
            refreshButton.setEnabled(true);
        }
    }

    private void refreshSelectionCount() {
        int selectedCount = repositoryTableModel.getSelectedRepositories().size();
        selectionCountLabel.setText("已选择 " + selectedCount + " 个仓库");
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
}
