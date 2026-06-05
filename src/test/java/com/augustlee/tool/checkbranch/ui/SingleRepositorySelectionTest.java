package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.WorkspaceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验单仓库选择逻辑。
 *
 * @author August Lee
 */
class SingleRepositorySelectionTest {

    /**
     * 验证仅选择一个仓库时，其余仓库会被取消选择。
     */
    @Test
    void shouldSelectOnlyOneRepository() {
        RepositoryTableModel tableModel = new RepositoryTableModel();
        WorkspaceRepository serviceOrder = new WorkspaceRepository("repo-order", "订单服务", "E:/workspace/order");
        WorkspaceRepository serviceUser = new WorkspaceRepository("repo-user", "用户服务", "E:/workspace/user");
        serviceOrder.setSelected(true);
        serviceUser.setSelected(true);
        tableModel.setRepositories(List.of(serviceOrder, serviceUser));

        tableModel.selectOnly("repo-user");

        assertFalse(serviceOrder.isSelected());
        assertTrue(serviceUser.isSelected());
        assertEquals(1, tableModel.getSelectedRepositories().size());
    }

    /**
     * 验证可以一键全选与取消全选所有仓库。
     */
    @Test
    void shouldToggleAllRepositoriesSelection() {
        RepositoryTableModel tableModel = new RepositoryTableModel();
        WorkspaceRepository serviceOrder = new WorkspaceRepository("repo-order", "订单服务", "E:/workspace/order");
        WorkspaceRepository serviceUser = new WorkspaceRepository("repo-user", "用户服务", "E:/workspace/user");
        tableModel.setRepositories(List.of(serviceOrder, serviceUser));

        tableModel.setAllSelected(true);
        assertEquals(2, tableModel.getSelectedRepositories().size());

        tableModel.setAllSelected(false);
        assertTrue(tableModel.getSelectedRepositories().isEmpty());
    }
}
