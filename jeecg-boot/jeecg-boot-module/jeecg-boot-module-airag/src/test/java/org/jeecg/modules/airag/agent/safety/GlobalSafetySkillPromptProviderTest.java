package org.jeecg.modules.airag.agent.safety;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GlobalSafetySkillPromptProviderTest {

    @Test
    void shouldRemoveFrontmatterAndPrependSafetyPrompt() {
        SkillRegistry skillRegistry = Mockito.mock(SkillRegistry.class);
        Mockito.when(skillRegistry.getSkillBody(GlobalSafetySkillPromptProvider.SKILL_CODE))
                .thenReturn("---\ncode: ai_safety_guard\nversion: \"1.0.0\"\n---\n\n# 安全规则\n\n始终遵守。");
        GlobalSafetySkillPromptProvider provider = new GlobalSafetySkillPromptProvider(skillRegistry);

        String prompt = provider.prependToSystemPrompt("业务规则");

        Assertions.assertEquals("# 安全规则\n\n始终遵守。\n\n业务规则", prompt);
        Assertions.assertFalse(prompt.contains("code: ai_safety_guard"));
    }

    @Test
    void shouldBuildSafeImageGenerationPrompt() {
        SkillRegistry skillRegistry = Mockito.mock(SkillRegistry.class);
        Mockito.when(skillRegistry.getSkillBody(GlobalSafetySkillPromptProvider.SKILL_CODE))
                .thenReturn("# 安全规则\n\n始终遵守。");
        GlobalSafetySkillPromptProvider provider = new GlobalSafetySkillPromptProvider(skillRegistry);

        String prompt = provider.buildImageGenerationPrompt("一座雪山");

        Assertions.assertTrue(prompt.startsWith("# 安全规则"));
        Assertions.assertTrue(prompt.contains("当前任务是生成图片"));
        Assertions.assertTrue(prompt.endsWith("原始图片提示词：\n一座雪山"));
    }

    @Test
    void shouldFailClosedWhenSafetySkillIsMissing() {
        SkillRegistry skillRegistry = Mockito.mock(SkillRegistry.class);
        Mockito.when(skillRegistry.getSkillBody(GlobalSafetySkillPromptProvider.SKILL_CODE))
                .thenThrow(new JeecgBootException("未找到Skill"));
        GlobalSafetySkillPromptProvider provider = new GlobalSafetySkillPromptProvider(skillRegistry);

        JeecgBootException exception = Assertions.assertThrows(
                JeecgBootException.class,
                provider::requiredSafetyPrompt
        );

        Assertions.assertTrue(exception.getMessage().contains("禁止调用AI模型"));
    }
}
