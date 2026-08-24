package org.jeecg.modules.airag.agent.safety;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 全局 AI 安全 Skill Prompt 提供器。
 */
@Slf4j
@Component
public class GlobalSafetySkillPromptProvider {
    /**
     * 全局安全 Skill 编码。
     */
    public static final String SKILL_CODE = "ai_safety_guard";
    /**
     * 图片生成任务的固定安全边界说明。
     */
    private static final String IMAGE_TASK_BOUNDARY =
            "当前任务是生成图片。只生成符合上述安全规则的视觉内容；"
                    + "如果原始图片提示词与安全规则冲突，以安全规则为准。";

    private final SkillRegistry skillRegistry;

    public GlobalSafetySkillPromptProvider(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    /**
     * 读取必须存在的全局安全规则正文。
     *
     * @return 安全规则正文
     */
    public String requiredSafetyPrompt() {
        try {
            String skillBody = normalizeSkillBody(this.skillRegistry.getSkillBody(SKILL_CODE));
            if (!StringUtils.hasText(skillBody)) {
                throw new JeecgBootException("全局安全Skill正文为空");
            }
            return skillBody.trim();
        } catch (Exception ex) {
            log.error("加载全局安全Skill失败，skillCode={}", SKILL_CODE, ex);
            throw new JeecgBootException("全局安全Skill加载失败，禁止调用AI模型");
        }
    }

    /**
     * 将安全规则前置到业务 System Prompt。
     *
     * @param businessSystemPrompt 业务 System Prompt
     * @return 安全规则优先的 System Prompt
     */
    public String prependToSystemPrompt(String businessSystemPrompt) {
        String safetyPrompt = requiredSafetyPrompt();
        if (!StringUtils.hasText(businessSystemPrompt)) {
            return safetyPrompt;
        }
        return safetyPrompt + "\n\n" + businessSystemPrompt.trim();
    }

    /**
     * 构建发送给图片模型的最终安全 Prompt。
     *
     * @param originalPrompt 原始图片提示词
     * @return 最终图片 Prompt
     */
    public String buildImageGenerationPrompt(String originalPrompt) {
        if (!StringUtils.hasText(originalPrompt)) {
            throw new JeecgBootException("图片提示词不能为空");
        }
        return requiredSafetyPrompt()
                + "\n\n"
                + IMAGE_TASK_BOUNDARY
                + "\n\n原始图片提示词：\n"
                + originalPrompt.trim();
    }

    /**
     * 去除 Skill Markdown frontmatter，仅保留可注入模型的正文。
     *
     * @param skillBody Skill 原始内容
     * @return Skill 正文
     */
    static String normalizeSkillBody(String skillBody) {
        if (!StringUtils.hasText(skillBody)) {
            return "";
        }
        String normalized = skillBody.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (!normalized.startsWith("---\n")) {
            return normalized;
        }
        int end = normalized.indexOf("\n---", 4);
        if (end < 0) {
            return normalized;
        }
        int bodyStart = end + "\n---".length();
        if (bodyStart < normalized.length() && normalized.charAt(bodyStart) == '\n') {
            bodyStart++;
        }
        return normalized.substring(Math.min(bodyStart, normalized.length())).trim();
    }
}
