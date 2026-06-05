package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验仓库表格模型的展示逻辑与选择行为。
 *
 * @author August Lee
 */
class RepositoryTableModelTest {

    /**
     * 验证表格模型能正确展示仓库状态字段。
     */
    @Test
    void shouldExposeRepositoryColumns() {
        RepositoryTableModel tableModel = new RepositoryTableModel();
        WorkspaceRepository repository = new WorkspaceRepository("repo-1", "订单服务", "E:/workspace/order");
        repository.setCurrentBranch("main");
        repository.setTargetBranchState("目标分支存在");
        repository.setHasUncommittedChanges(true);
        repository.setOperationBlocked(false);
        repository.setLastOperationResultSummary("暂无结果");
        tableModel.setRepositories(List.of(repository));

        assertEquals(1, tableModel.getRowCount());
        assertEquals("订单服务", tableModel.getValueAt(0, 1));
        assertEquals("main", tableModel.getValueAt(0, 2));
        assertEquals("目标分支存在", tableModel.getValueAt(0, 3));
        assertEquals(RepositoryTableModel.RepositoryStatusColor.WARNING, tableModel.getStatusColorAt(0));
    }

    /**
     * 验证表格模型能正确维护仓库选择状态和结果摘要。
     */
    @Test
    void shouldUpdateSelectionAndLastResultSummary() {
        RepositoryTableModel tableModel = new RepositoryTableModel();
        WorkspaceRepository repository = new WorkspaceRepository("repo-1", "订单服务", "E:/workspace/order");
        tableModel.setRepositories(List.of(repository));

        assertFalse(repository.isSelected());
        tableModel.setValueAt(Boolean.TRUE, 0, 0);
        tableModel.updateLastResultSummary("repo-1", "刷新完成");

        assertTrue(repository.isSelected());
        assertEquals(1, tableModel.getSelectedRepositories().size());
        assertEquals("正常", tableModel.getValueAt(0, 3));
    }

    /**
     * 验证单独取消某个仓库勾选时，不会影响其他仓库。
     */
    @Test
    void shouldUncheckSingleRepositoryWithoutAffectingOthers() {
        RepositoryTableModel tableModel = new RepositoryTableModel();
        WorkspaceRepository serviceOrder = new WorkspaceRepository("repo-order", "订单服务", "E:/workspace/order");
        WorkspaceRepository serviceUser = new WorkspaceRepository("repo-user", "用户服务", "E:/workspace/user");
        serviceOrder.setSelected(true);
        serviceUser.setSelected(true);
        tableModel.setRepositories(List.of(serviceOrder, serviceUser));

        tableModel.setValueAt(Boolean.FALSE, 0, 0);

        assertFalse(serviceOrder.isSelected());
        assertTrue(serviceUser.isSelected());
        assertEquals(1, tableModel.getSelectedRepositories().size());
    }
}
