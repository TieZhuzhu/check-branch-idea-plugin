package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.SwitchResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.util.List;

/**
 * 展示分支切换结果摘要的面板骨架。
 *
 * @author August Lee
 */
public class SwitchResultPanel extends JPanel {

    private final DefaultListModel<String> resultListModel = new DefaultListModel<>();
    private final JList<String> resultList = new JList<>(resultListModel);

    /**
     * 创建结果面板。
     */
    public SwitchResultPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                "切换结果",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        resultList.getAccessibleContext().setAccessibleName("切换结果列表");
        add(new JScrollPane(resultList), BorderLayout.CENTER);
        showEmptyState("暂无切换结果");
    }

    /**
     * 按仓库列表展示结果摘要。
     *
     * @param results 切换结果列表
     */
    public void showResults(List<SwitchResult> results) {
        resultListModel.clear();
        if (results == null || results.isEmpty()) {
            showEmptyState("暂无切换结果");
            return;
        }
        for (SwitchResult result : results) {
            resultListModel.addElement(result.toSummaryText());
        }
    }

    /**
     * 清空当前结果面板内容。
     */
    public void clearResults() {
        showEmptyState("暂无切换结果");
    }

    /**
     * 展示空状态提示文本。
     *
     * @param message 空状态提示文本
     */
    public void showEmptyState(String message) {
        resultListModel.clear();
        resultListModel.addElement(message);
    }
}
