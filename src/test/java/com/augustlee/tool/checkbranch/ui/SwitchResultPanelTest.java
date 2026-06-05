package com.augustlee.tool.checkbranch.ui;

import com.augustlee.tool.checkbranch.model.SwitchResult;
import com.augustlee.tool.checkbranch.model.SwitchResultStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 校验结果面板的摘要与过滤展示行为。
 *
 * @author August Lee
 */
class SwitchResultPanelTest {

    /**
     * 验证单仓库和批量结果的摘要标题会自动切换。
     */
    @Test
    void shouldShowSingleAndBatchSummaryText() {
        SwitchResultPanel panel = new SwitchResultPanel();
        SwitchResult orderResult = createResult("repo-order", SwitchResultStatus.SUCCESS, "feature/demo", "feature/demo");
        SwitchResult userResult = createResult("repo-user", SwitchResultStatus.FALLBACK_TO_MAIN, "feature/demo", "main");

        panel.showResults(List.of(orderResult));
        assertEquals("单仓库切换", panel.getSummaryText());
        assertEquals(1, panel.getChipCount());

        panel.showResults(List.of(orderResult, userResult));
        assertEquals("批量切换", panel.getSummaryText());
        assertEquals(2, panel.getChipCount());
    }

    /**
     * 验证结果面板可以按仓库标识过滤结果。
     */
    @Test
    void shouldFilterResultsByRepositoryIds() {
        SwitchResultPanel panel = new SwitchResultPanel();
        SwitchResult orderResult = createResult("repo-order", SwitchResultStatus.SUCCESS, "feature/demo", "feature/demo");
        SwitchResult userResult = createResult("repo-user", SwitchResultStatus.FALLBACK_TO_MAIN, "feature/demo", "main");

        panel.showResultsForRepositories(List.of(orderResult, userResult), List.of("repo-order"));

        assertEquals("单仓库切换", panel.getSummaryText());
        assertEquals(1, panel.getChipCount());
    }

    /**
     * 验证结果卡片标题使用仓库名而不是完整路径。
     */
    @Test
    void shouldUseRepositoryNameInsteadOfFullPath() {
        SwitchResultPanel panel = new SwitchResultPanel();
        SwitchResult result = createResult("E:/workspace/project/reco-sgb2b/mallapi", SwitchResultStatus.SUCCESS, "feature/demo", "feature/demo");

        panel.showResults(List.of(result));

        assertEquals(1, panel.getChipCount());
        assertEquals("单仓库切换", panel.getSummaryText());
    }

    private SwitchResult createResult(String repositoryId, SwitchResultStatus status, String requestedBranch, String finalBranch) {
        return new SwitchResult(
                repositoryId,
                status,
                requestedBranch,
                finalBranch,
                status == SwitchResultStatus.FALLBACK_TO_MAIN,
                "",
                "测试结果",
                status == SwitchResultStatus.FAILED ? "失败原因" : "",
                Instant.now(),
                "本地分支"
        );
    }
}
