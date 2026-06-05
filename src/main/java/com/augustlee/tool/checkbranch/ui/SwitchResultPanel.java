package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.SwitchResult;
import com.augustlee.tool.checkbranch.model.SwitchResultStatus;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 展示分支切换结果摘要的面板骨架。
 *
 * @author August Lee
 */
public class SwitchResultPanel extends JPanel {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages.CheckBranchBundle");
    private final JLabel summaryLabel = new JLabel(BUNDLE.getString("label.result.summary.none"));
    private final JPanel chipsPanel = new JPanel(new GridLayout(0, 3, 8, 8));

    /**
     * 创建结果面板。
     */
    public SwitchResultPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                BUNDLE.getString("label.result.title"),
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        JPanel contentPanel = new JPanel(new BorderLayout(0, 8));
        summaryLabel.getAccessibleContext().setAccessibleName("切换结果摘要");
        contentPanel.add(summaryLabel, BorderLayout.NORTH);
        contentPanel.add(chipsPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
        showEmptyState(BUNDLE.getString("label.result.empty"));
    }

    /**
     * 按仓库列表展示结果摘要。
     *
     * @param results 切换结果列表
     */
    public void showResults(List<SwitchResult> results) {
        chipsPanel.removeAll();
        if (results == null || results.isEmpty()) {
            showEmptyState(BUNDLE.getString("label.result.empty"));
            return;
        }
        summaryLabel.setText(results.size() == 1
                ? BUNDLE.getString("label.result.summary.single")
                : BUNDLE.getString("label.result.summary.batch"));
        for (SwitchResult result : results) {
            chipsPanel.add(createResultChip(result));
        }
        chipsPanel.revalidate();
        chipsPanel.repaint();
    }

    /**
     * 按指定仓库标识过滤并展示结果。
     *
     * @param results 原始结果列表
     * @param repositoryIds 允许展示的仓库标识
     */
    public void showResultsForRepositories(List<SwitchResult> results, List<String> repositoryIds) {
        if (results == null || repositoryIds == null || repositoryIds.isEmpty()) {
            showEmptyState(BUNDLE.getString("label.result.empty"));
            return;
        }
        List<SwitchResult> filteredResults = results.stream()
                .filter(result -> repositoryIds.contains(result.getRepositoryId()))
                .toList();
        showResults(filteredResults);
    }

    /**
     * 清空当前结果面板内容。
     */
    public void clearResults() {
        showEmptyState(BUNDLE.getString("label.result.empty"));
    }

    /**
     * 展示空状态提示文本。
     *
     * @param message 空状态提示文本
     */
    public void showEmptyState(String message) {
        summaryLabel.setText(message);
        chipsPanel.removeAll();
        chipsPanel.revalidate();
        chipsPanel.repaint();
    }

    /**
     * 返回当前结果摘要标题文本，便于测试与状态同步。
     *
     * @return 当前结果摘要标题文本
     */
    public String getSummaryText() {
        return summaryLabel.getText();
    }

    /**
     * 返回当前结果卡片数量，便于测试与状态同步。
     *
     * @return 当前结果卡片数量
     */
    public int getChipCount() {
        return chipsPanel.getComponentCount();
    }

    private JPanel createResultChip(SwitchResult result) {
        JPanel chipPanel = new JPanel();
        chipPanel.setLayout(new BoxLayout(chipPanel, BoxLayout.Y_AXIS));
        chipPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(resolveColor(result.getStatus()).darker()),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        chipPanel.setBackground(resolveColor(result.getStatus()));
        chipPanel.setToolTipText(buildTooltipText(result));

        JLabel titleLabel = new JLabel(result.getRepositoryDisplayName());
        JLabel detailLabel = new JLabel(resolveStatusText(result));
        JLabel branchLabel = new JLabel(result.getFinalBranch().isEmpty() ? result.getRequestedBranch() : result.getFinalBranch());
        JLabel failureLabel = new JLabel(resolveFailureReason(result));
        JLabel restoreLabel = new JLabel(resolveRestoreHint(result));

        titleLabel.setForeground(Color.BLACK);
        detailLabel.setForeground(new Color(70, 70, 70));
        branchLabel.setForeground(new Color(90, 90, 90));
        failureLabel.setForeground(new Color(120, 40, 40));
        restoreLabel.setForeground(new Color(120, 40, 40));
        String tooltipText = buildTooltipText(result);
        titleLabel.setToolTipText(tooltipText);
        detailLabel.setToolTipText(tooltipText);
        branchLabel.setToolTipText(tooltipText);
        failureLabel.setToolTipText(tooltipText);
        restoreLabel.setToolTipText(tooltipText);

        chipPanel.add(titleLabel);
        chipPanel.add(Box.createVerticalStrut(4));
        chipPanel.add(detailLabel);
        chipPanel.add(Box.createVerticalStrut(2));
        chipPanel.add(branchLabel);
        if (!failureLabel.getText().isEmpty()) {
            chipPanel.add(Box.createVerticalStrut(2));
            chipPanel.add(failureLabel);
        }
        if (!restoreLabel.getText().isEmpty()) {
            chipPanel.add(Box.createVerticalStrut(2));
            chipPanel.add(restoreLabel);
        }
        return chipPanel;
    }

    private String resolveStatusText(SwitchResult result) {
        return switch (result.getStatus()) {
            case SUCCESS -> BUNDLE.getString("result.summary.success");
            case FALLBACK_TO_MAIN -> BUNDLE.getString("result.summary.fallback");
            case SKIPPED -> BUNDLE.getString("result.summary.skipped");
            case FAILED -> BUNDLE.getString("result.summary.failed");
            case MANUAL_RESTORE_REQUIRED -> BUNDLE.getString("result.summary.restore");
        };
    }

    private String resolveRestoreHint(SwitchResult result) {
        if (result.getStatus() != SwitchResultStatus.MANUAL_RESTORE_REQUIRED) {
            return "";
        }
        return BUNDLE.getString("result.summary.restore.hint")
                + (result.getTemporaryChangeRecordId().isBlank() ? "" : "：" + result.getTemporaryChangeRecordId());
    }

    private String resolveFailureReason(SwitchResult result) {
        if (result.getStatus() != SwitchResultStatus.FAILED || result.getFailureReason().isBlank()) {
            return "";
        }
        return "原因：" + result.getFailureReason();
    }

    private String buildTooltipText(SwitchResult result) {
        StringBuilder tooltipBuilder = new StringBuilder("<html><body style='width:360px'>")
                .append("仓库：").append(escapeHtml(result.getRepositoryDisplayName()))
                .append("<br/>状态：").append(escapeHtml(resolveStatusText(result)))
                .append("<br/>请求分支：").append(escapeHtml(result.getRequestedBranch()))
                .append("<br/>最终分支：").append(escapeHtml(result.getFinalBranch().isEmpty() ? "未切换" : result.getFinalBranch()));
        if (!result.getBranchSource().isBlank()) {
            tooltipBuilder.append("<br/>分支来源：").append(escapeHtml(result.getBranchSource()));
        }
        if (!result.getMessage().isBlank()) {
            tooltipBuilder.append("<br/>说明：").append(escapeHtml(result.getMessage()));
        }
        if (!result.getFailureReason().isBlank()) {
            tooltipBuilder.append("<br/>失败原因：").append(escapeHtml(result.getFailureReason()));
        }
        tooltipBuilder.append("</body></html>");
        return tooltipBuilder.toString();
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private Color resolveColor(SwitchResultStatus status) {
        return switch (status) {
            case SUCCESS -> new Color(232, 245, 233);
            case FALLBACK_TO_MAIN -> new Color(255, 243, 224);
            case SKIPPED -> new Color(245, 245, 245);
            case FAILED, MANUAL_RESTORE_REQUIRED -> new Color(255, 235, 238);
        };
    }
}
