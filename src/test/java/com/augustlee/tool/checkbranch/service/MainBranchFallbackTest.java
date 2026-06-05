package com.augustlee.tool.checkbranch.service;

import com.augustlee.tool.checkbranch.model.ChangeProtectionMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验主分支候选顺序与目标分支输入规则。
 *
 * @author August Lee
 */
class MainBranchFallbackTest {

    /**
     * 验证默认主分支候选顺序为 main、master。
     */
    @Test
    void shouldExposeDefaultMainBranchCandidates() {
        BranchPreferenceService branchPreferenceService = new BranchPreferenceService();

        assertEquals(List.of("main", "master"), branchPreferenceService.getMainBranchCandidates());
    }

    /**
     * 验证可以覆盖主分支候选顺序。
     */
    @Test
    void shouldAllowOverrideMainBranchCandidates() {
        BranchPreferenceService branchPreferenceService = new BranchPreferenceService();

        branchPreferenceService.setMainBranchNames(List.of("develop", "main", "master"));

        assertEquals(List.of("develop", "main", "master"), branchPreferenceService.getMainBranchCandidates());
    }

    /**
     * 验证目标分支输入规则可以识别非法字符。
     */
    @Test
    void shouldValidateTargetBranchInput() {
        BranchPreferenceService branchPreferenceService = new BranchPreferenceService();

        assertTrue(branchPreferenceService.isValidTargetBranch("feature/demo"));
        assertFalse(branchPreferenceService.isValidTargetBranch("feature demo"));
        assertFalse(branchPreferenceService.isValidTargetBranch("feature:demo"));
        assertEquals(ChangeProtectionMode.ASK, branchPreferenceService.getDefaultChangeProtectionMode());
    }
}
