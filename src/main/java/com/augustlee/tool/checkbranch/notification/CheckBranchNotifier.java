package com.augustlee.tool.checkbranch.notification;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * 统一封装插件中的中文通知提示能力。
 *
 * @author August Lee
 */
public class CheckBranchNotifier {

    private static final String NOTIFICATION_GROUP_ID = "checkbranch.notification";

    /**
     * 发送普通提示通知。
     *
     * @param project 当前 IDEA 项目；允许为空
     * @param title 通知标题
     * @param content 通知内容
     */
    public void notifyInfo(@Nullable Project project, String title, String content) {
        notify(project, title, content, NotificationType.INFORMATION);
    }

    /**
     * 发送警告通知。
     *
     * @param project 当前 IDEA 项目；允许为空
     * @param title 通知标题
     * @param content 通知内容
     */
    public void notifyWarning(@Nullable Project project, String title, String content) {
        notify(project, title, content, NotificationType.WARNING);
    }

    /**
     * 发送错误通知。
     *
     * @param project 当前 IDEA 项目；允许为空
     * @param title 通知标题
     * @param content 通知内容
     */
    public void notifyError(@Nullable Project project, String title, String content) {
        notify(project, title, content, NotificationType.ERROR);
    }

    private void notify(@Nullable Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, content, type)
                .notify(project);
    }
}
