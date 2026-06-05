package com.augustlee.tool.checkbranch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 校验阶段 2 模型对象的最小约束逻辑。
 *
 * @author August Lee
 */
class BranchSwitchModelTest {

    /**
     * 验证工作区仓库模型会自动回填展示名称。
     */
    @Test
    void shouldDeriveDisplayNameWhenDisplayNameIsBlank() {
        WorkspaceRepository repository = new WorkspaceRepository("repo-1", " ", "E:/workspace/service-order");

        repository.validate();

        assertEquals("service-order", repository.getDisplayName());
    }

    /**
     * 验证分支切换请求在目标分支为空时会抛出异常。
     */
    @Test
    void shouldRejectBlankTargetBranch() {
        assertThrows(IllegalArgumentException.class, () -> new BranchSwitchRequest(
                "request-1",
                " ",
                List.of("repo-1"),
                ChangeProtectionMode.ASK,
                Instant.now(),
                false
        ));
    }

    /**
     * 验证临时变更记录要求使用实际保存方式。
     */
    @Test
    void shouldRejectAskModeForTemporaryChangeRecord() {
        assertThrows(IllegalArgumentException.class, () -> new TemporaryChangeRecord(
                "record-1",
                "repo-1",
                ChangeProtectionMode.ASK,
                "未提交变更",
                Instant.now(),
                "请手动恢复",
                true
        ));
    }

    /**
     * 验证失败结果必须携带失败原因。
     */
    @Test
    void shouldRequireFailureReasonForFailedResult() {
        assertThrows(IllegalArgumentException.class, () -> new SwitchResult(
                "repo-1",
                SwitchResultStatus.FAILED,
                "feature/demo",
                "",
                false,
                "",
                "切换失败",
                "",
                Instant.now(),
                "本地分支"
        ));
    }

    /**
     * 验证合法的模型对象可以正常通过校验。
     */
    @Test
    void shouldAcceptValidModels() {
        WorkspaceRepository repository = new WorkspaceRepository("repo-1", "订单服务", "E:/workspace/order");
        BranchSwitchRequest request = new BranchSwitchRequest(
                "request-1",
                "feature/demo",
                List.of("repo-1"),
                ChangeProtectionMode.SHELVE,
                Instant.now(),
                false
        );
        TemporaryChangeRecord record = new TemporaryChangeRecord(
                "record-1",
                "repo-1",
                ChangeProtectionMode.GIT_STASH,
                "订单服务暂存",
                Instant.now(),
                "请执行 stash apply 恢复变更",
                true
        );
        SwitchResult result = new SwitchResult(
                "repo-1",
                SwitchResultStatus.SUCCESS,
                "feature/demo",
                "feature/demo",
                false,
                "",
                "切换成功",
                "",
                Instant.now(),
                "本地分支"
        );

        assertDoesNotThrow(repository::validate);
        assertDoesNotThrow(request::validate);
        assertDoesNotThrow(record::validate);
        assertDoesNotThrow(result::validate);
    }
}
