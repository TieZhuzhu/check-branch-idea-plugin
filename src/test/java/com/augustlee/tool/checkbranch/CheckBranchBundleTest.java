package com.augustlee.tool.checkbranch;

import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验中文资源文件的关键键值是否完整可用。
 *
 * @author August Lee
 */
class CheckBranchBundleTest {

    /**
     * 验证关键资源键已经定义。
     */
    @Test
    void shouldContainRequiredResourceKeys() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages.CheckBranchBundle");

        assertTrue(bundle.containsKey("plugin.name"));
        assertTrue(bundle.containsKey("toolwindow.title"));
        assertTrue(bundle.containsKey("action.refresh"));
        assertTrue(bundle.containsKey("action.switch"));
        assertTrue(bundle.containsKey("status.loading"));
        assertTrue(bundle.containsKey("notification.title.error"));
    }

    /**
     * 验证部分关键中文文案符合当前约定。
     */
    @Test
    void shouldExposeChineseMessages() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages.CheckBranchBundle");

        assertEquals("Easy Multi Project Checkout Branch", bundle.getString("plugin.name"));
        assertEquals("分支切换", bundle.getString("toolwindow.title"));
        assertEquals("执行切换", bundle.getString("action.switch"));
    }
}
