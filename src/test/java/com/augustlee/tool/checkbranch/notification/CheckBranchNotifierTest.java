package com.augustlee.tool.checkbranch.notification;

import com.intellij.notification.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 校验通知服务构建出的通知对象信息。
 *
 * @author August Lee
 */
class CheckBranchNotifierTest {

    /**
     * 验证通知对象包含预期标题、内容和类型。
     */
    @Test
    void shouldBuildNotificationWithExpectedContent() {
        CheckBranchNotifier notifier = new CheckBranchNotifier();

        CheckBranchNotifier.NotificationPayload payload = notifier.buildPayload("标题", "内容", NotificationType.WARNING);

        assertEquals("标题", payload.title());
        assertEquals("内容", payload.content());
        assertEquals(NotificationType.WARNING, payload.type());
    }
}
