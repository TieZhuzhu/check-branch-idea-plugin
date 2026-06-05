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
            "当前分支",
            "状态"
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
            case 2 -> repository.getCurrentBranch();
            case 3 -> resolveStatusText(repository);
            default -> "";
        };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 0 || rowIndex < 0 || rowIndex >= repositories.size()) {
            return;
        }
        boolean selected = Boolean.TRUE.equals(aValue);
        repositories.get(rowIndex).setSelected(selected);
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
     * 仅选择指定仓库，其余仓库全部取消选择。
     *
     * @param repositoryId 需要保留选中状态的仓库标识
     */
    public void selectOnly(String repositoryId) {
        for (int index = 0; index < repositories.size(); index++) {
            WorkspaceRepository repository = repositories.get(index);
            repository.setSelected(repository.getId().equals(repositoryId));
        }
        fireTableDataChanged();
    }

    /**
     * 返回当前所有仓库是否均被选中。
     *
     * @return 全部选中返回 {@code true}
     */
    public boolean isAllSelected() {
        return !repositories.isEmpty() && repositories.stream().allMatch(WorkspaceRepository::isSelected);
    }

    /**
     * 批量设置所有仓库的选择状态。
     *
     * @param selected 目标选择状态
     */
    public void setAllSelected(boolean selected) {
        for (WorkspaceRepository repository : repositories) {
            repository.setSelected(selected);
        }
        fireTableDataChanged();
    }

    /**
     * 刷新当前所有仓库行，适用于仓库对象字段已在外部更新的场景。
     */
    public void refreshAllRows() {
        fireTableDataChanged();
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

    /**
     * 返回指定仓库所在行的状态颜色。
     *
     * @param rowIndex 行号
     * @return 状态颜色类型
     */
    public RepositoryStatusColor getStatusColorAt(int rowIndex) {
        WorkspaceRepository repository = repositories.get(rowIndex);
        if (repository.isOperationBlocked()) {
            return RepositoryStatusColor.DANGER;
        }
        if (repository.hasUncommittedChanges()) {
            return RepositoryStatusColor.WARNING;
        }
        return RepositoryStatusColor.SUCCESS;
    }

    private String resolveStatusText(WorkspaceRepository repository) {
        if (repository.isOperationBlocked()) {
            return repository.getBlockReason();
        }
        if (repository.getTargetBranchState() != null
                && !repository.getTargetBranchState().isBlank()
                && !"未知".equals(repository.getTargetBranchState())) {
            return repository.getTargetBranchState();
        }
        if (repository.hasUncommittedChanges()) {
            return "有变更";
        }
        return "正常";
    }

    /**
     * 表示仓库状态对应的颜色类型。
     *
     * @author August Lee
     */
    public enum RepositoryStatusColor {
        /**
         * 绿色，表示仓库状态正常。
         */
        SUCCESS,

        /**
         * 橙色，表示仓库存在未提交变更。
         */
        WARNING,

        /**
         * 红色，表示仓库当前不可切换。
         */
        DANGER
    }
}
