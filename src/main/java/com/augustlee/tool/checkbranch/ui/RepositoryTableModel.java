package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具窗口中仓库列表表格的数据模型。
 *
 * @author August Lee
 */
public class RepositoryTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
            "参与切换",
            "仓库名称",
            "仓库路径",
            "当前分支",
            "目标分支状态",
            "变更状态",
            "可切换状态",
            "最近结果"
    };

    private final List<WorkspaceRepository> repositories = new ArrayList<>();

    /**
     * 返回当前表格中是否存在仓库数据。
     *
     * @return 是否存在仓库数据
     */
    public boolean isEmpty() {
        return repositories.isEmpty();
    }

    @Override
    public int getRowCount() {
        return repositories.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        WorkspaceRepository repository = repositories.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> repository.isSelected();
            case 1 -> repository.getDisplayName();
            case 2 -> repository.getRootPath();
            case 3 -> repository.getCurrentBranch();
            case 4 -> repository.getTargetBranchState();
            case 5 -> repository.hasUncommittedChanges() ? "有未提交变更" : "工作区干净";
            case 6 -> repository.isOperationBlocked() ? repository.getBlockReason() : "可切换";
            case 7 -> repository.getLastOperationResultSummary().isEmpty()
                    ? "暂无结果"
                    : repository.getLastOperationResultSummary();
            default -> "";
        };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 0 || rowIndex < 0 || rowIndex >= repositories.size()) {
            return;
        }
        repositories.get(rowIndex).setSelected(Boolean.TRUE.equals(aValue));
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    /**
     * 使用新的仓库列表刷新表格数据。
     *
     * @param repositories 仓库列表
     */
    public void setRepositories(List<WorkspaceRepository> repositories) {
        this.repositories.clear();
        if (repositories != null) {
            this.repositories.addAll(repositories);
        }
        fireTableDataChanged();
    }

    /**
     * 返回当前表格中的所有仓库。
     *
     * @return 仓库列表副本
     */
    public List<WorkspaceRepository> getRepositories() {
        return new ArrayList<>(repositories);
    }

    /**
     * 返回当前已勾选的仓库列表。
     *
     * @return 已勾选仓库列表
     */
    public List<WorkspaceRepository> getSelectedRepositories() {
        return repositories.stream()
                .filter(WorkspaceRepository::isSelected)
                .toList();
    }

    /**
     * 更新指定仓库的最近结果摘要。
     *
     * @param repositoryId 仓库标识
     * @param summary 最近结果摘要
     */
    public void updateLastResultSummary(String repositoryId, String summary) {
        for (int index = 0; index < repositories.size(); index++) {
            WorkspaceRepository repository = repositories.get(index);
            if (repository.getId().equals(repositoryId)) {
                repository.setLastOperationResultSummary(summary);
                fireTableRowsUpdated(index, index);
                return;
            }
        }
    }
}
