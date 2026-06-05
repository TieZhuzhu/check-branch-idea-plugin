package com.augustlee.tool.checkbranch;

import com.augustlee.tool.checkbranch.ui.BranchSwitchToolWindowContent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 创建右侧“分支切换”工具窗口内容。
 *
 * @author August Lee
 */
public class CheckBranchToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        BranchSwitchToolWindowContent contentPanel = new BranchSwitchToolWindowContent(project);
        Content content = ContentFactory.getInstance().createContent(contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
