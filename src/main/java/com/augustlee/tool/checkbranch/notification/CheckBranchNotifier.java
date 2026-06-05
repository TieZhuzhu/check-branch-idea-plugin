package com.augustlee.tool.checkbranch.notification;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationGroup;
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

    /**
     * 构建通知元数据，便于测试与上层组合逻辑复用。
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     * @return 通知元数据
     */
    public NotificationPayload buildPayload(String title, String content, NotificationType type) {
        return new NotificationPayload(title, content, type);
    }

    /**
     * 创建但不立即发送通知，便于测试或组合调用。
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     * @return 创建出的通知对象
     */
    public com.intellij.notification.Notification buildNotification(String title, String content, NotificationType type) {
        NotificationPayload payload = buildPayload(title, content, type);
        NotificationGroup group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
        return group.createNotification(payload.title(), payload.content(), payload.type());
    }

    private void notify(@Nullable Project project, String title, String content, NotificationType type) {
        buildNotification(title, content, type).notify(project);
    }

    /**
     * 表示通知展示所需的核心元数据。
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型
     * @author August Lee
     */
    public record NotificationPayload(String title, String content, NotificationType type) {
    }
}
