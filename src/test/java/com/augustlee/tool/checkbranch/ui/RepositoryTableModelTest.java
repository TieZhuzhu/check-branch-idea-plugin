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
        assertEquals("main", tableModel.getValueAt(0, 3));
        assertEquals("有未提交变更", tableModel.getValueAt(0, 5));
        assertEquals("可切换", tableModel.getValueAt(0, 6));
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
        assertEquals("刷新完成", tableModel.getValueAt(0, 7));
    }
}
