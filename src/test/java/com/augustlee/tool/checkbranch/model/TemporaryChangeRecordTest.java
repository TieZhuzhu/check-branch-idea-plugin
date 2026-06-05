package com.augustlee.tool.checkbranch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验临时变更记录的字段约束和恢复提示。
 *
 * @author August Lee
 */
class TemporaryChangeRecordTest {

    /**
     * 验证实际保存方式可以创建有效临时记录。
     */
    @Test
    void shouldCreateTemporaryChangeRecord() {
        TemporaryChangeRecord record = new TemporaryChangeRecord(
                "record-1",
                "repo-1",
                ChangeProtectionMode.GIT_STASH,
                "Git 暂存记录",
                Instant.now(),
                "请在 Git 工具窗口中手动恢复暂存栈",
                true
        );

        assertEquals("record-1", record.getRecordId());
        assertEquals(ChangeProtectionMode.GIT_STASH, record.getMethod());
        assertTrue(record.isRequiresManualRestore());
    }

    /**
     * 验证询问和跳过模式不能作为临时保存方式。
     */
    @Test
    void shouldRejectNonSavingMode() {
        assertThrows(IllegalArgumentException.class, () -> new TemporaryChangeRecord(
                "record-2",
                "repo-1",
                ChangeProtectionMode.SKIP,
                "跳过",
                Instant.now(),
                "无恢复提示",
                true
        ));
    }
}
