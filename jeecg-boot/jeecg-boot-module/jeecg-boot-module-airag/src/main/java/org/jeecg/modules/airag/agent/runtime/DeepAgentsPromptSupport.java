package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.jeecg.modules.airag.agent.skill.model.SkillLoadResult;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * DeepAgents 主链路提示词支持。
 *
 * @author codex
 * @date 2026/7/9
 */
public final class DeepAgentsPromptSupport {
    /**
     * deepagents task 系统提示词。
     */
    private static final String TASK_SYSTEM_PROMPT_PATH = "prompts/deepagents/task_system_prompt.txt";
    /**
     * deepagents skills 系统提示词。
     */
    private static final String SKILLS_SYSTEM_PROMPT_PATH = "prompts/deepagents/skills_system_prompt.txt";
    /**
     * task 系统提示词缓存。
     */
    private static volatile String cachedTaskSystemPrompt;
    /**
     * skills 系统提示词缓存。
     */
    private static volatile String cachedSkillsSystemPrompt;

    private DeepAgentsPromptSupport() {
    }

    /**
     * 是否启用 deepagents 风格提示词。
     *
     * @param context 上下文
     * @return true 表示启用
     */
    public static boolean isEnabled(AgentContext context) {
        if (context == null) {
            return false;
        }
        Object value = context.getAttribute("deepAgentsPromptMode");
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 是否为主 Agent 注入 DeepAgents 基础提示词。
     *
     * @param context 上下文
     * @return true 表示当前是主 Agent 节点
     */
    private static boolean isMainAgentPromptEnabled(AgentContext context) {
        if (!isEnabled(context)) {
            return false;
        }
        Object value = context.getAttribute("deepAgentsMainMode");
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 构建 deepagents 主链路基础提示词。
     *
     * @param context 运行上下文
     * @param skillLoadResult skill 结果
     * @return 提示词
     */
    public static String buildBasePrompt(AgentContext context, SkillLoadResult skillLoadResult) {
        if (!isMainAgentPromptEnabled(context)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendIfHasText(sb, loadTaskSystemPrompt());
        appendIfHasText(sb, buildSubAgentsPrompt(context));
        String skillsPrompt = renderSkillsSystemPrompt(context, skillLoadResult);
        appendIfHasText(sb, skillsPrompt);
        return sb.toString().trim();
    }

    /**
     * 读取 task 系统提示词。
     *
     * @return 提示词
     */
    public static String loadTaskSystemPrompt() {
        if (oConvertUtils.isNotEmpty(cachedTaskSystemPrompt)) {
            return cachedTaskSystemPrompt;
        }
        synchronized (DeepAgentsPromptSupport.class) {
            if (oConvertUtils.isNotEmpty(cachedTaskSystemPrompt)) {
                return cachedTaskSystemPrompt;
            }
            cachedTaskSystemPrompt = readClasspathText(TASK_SYSTEM_PROMPT_PATH);
            return cachedTaskSystemPrompt;
        }
    }

    /**
     * 渲染 skills 系统提示词。
     *
     * @param skillLoadResult skill 结果
     * @return 提示词
     */
    public static String renderSkillsSystemPrompt(AgentContext context, SkillLoadResult skillLoadResult) {
        if (skillLoadResult == null) {
            return "";
        }
        String skillList = buildSkillList(skillLoadResult.getCandidateSkills());
        if (!org.springframework.util.StringUtils.hasText(skillList)) {
            return "";
        }
        String template = loadSkillsSystemPrompt();
        if (!org.springframework.util.StringUtils.hasText(template)) {
            return skillList;
        }
        return template
                .replace("{skills_locations}", buildSkillLocations(context))
                .replace("{skills_load_warnings}", buildSkillWarnings(skillLoadResult))
                .replace("{skills_list}", skillList)
                .trim();
    }

    /**
     * 读取 skills 系统提示词。
     *
     * @return 提示词
     */
    public static String loadSkillsSystemPrompt() {
        if (oConvertUtils.isNotEmpty(cachedSkillsSystemPrompt)) {
            return cachedSkillsSystemPrompt;
        }
        synchronized (DeepAgentsPromptSupport.class) {
            if (oConvertUtils.isNotEmpty(cachedSkillsSystemPrompt)) {
                return cachedSkillsSystemPrompt;
            }
            cachedSkillsSystemPrompt = readClasspathText(SKILLS_SYSTEM_PROMPT_PATH);
            return cachedSkillsSystemPrompt;
        }
    }

    /**
     * skill 列表最多展示条数。
     */
    private static final int MAX_SKILL_LIST_SIZE = 5;
    /**
     * skill 描述最大长度。
     */
    private static final int MAX_SKILL_DESC_LENGTH = 80;

    /**
     * 组装 skill 列表。
     *
     * @param skills skill 列表
     * @return 文本
     */
    private static String buildSkillList(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(skills.size(), MAX_SKILL_LIST_SIZE);
        for (int i = 0; i < limit; i++) {
            SkillDefinition skill = skills.get(i);
            if (skill == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(i + 1).append(". ");
            sb.append(oConvertUtils.getString(skill.getCode()));
            sb.append(" | ").append(oConvertUtils.getString(skill.getName()));
            sb.append(" | ").append(shortenText(oConvertUtils.getString(skill.getDescription()), MAX_SKILL_DESC_LENGTH));
        }
        return sb.toString();
    }

    /**
     * 组装技能来源说明。
     *
     * @param context 上下文
     * @return 来源说明
     */
    private static String buildSkillLocations(AgentContext context) {
        String rootDir = context == null ? null : oConvertUtils.getString(context.getAttribute("skillRootDir"));
        if (!org.springframework.util.StringUtils.hasText(rootDir)) {
            return "";
        }
        return "Source: " + rootDir + "\n";
    }

    /**
     * 组装可用子 Agent 说明。
     *
     * @param context 运行上下文
     * @return 子 Agent 说明
     */
    private static String buildSubAgentsPrompt(AgentContext context) {
        if (context == null) {
            return "";
        }
        String availableSubAgents = oConvertUtils.getString(context.getAttribute("availableSubAgentsPrompt"));
        if (org.springframework.util.StringUtils.hasText(availableSubAgents)) {
            return "## Available Sub-Agents\n" + availableSubAgents.trim();
        }
        return "";
    }

    /**
     * 组装技能加载说明。
     *
     * @param skillLoadResult 加载结果
     * @return 说明文本
     */
    private static String buildSkillWarnings(SkillLoadResult skillLoadResult) {
        if (skillLoadResult == null || skillLoadResult.getMetadata() == null) {
            return "";
        }
        Object candidateCount = skillLoadResult.getMetadata().get("candidateCount");
        if (candidateCount == null) {
            return "";
        }
        return "Note: " + candidateCount + " candidate skill(s)\n";
    }

    /**
     * 截断文本。
     *
     * @param text 原文
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private static String shortenText(String text, int maxLength) {
        if (!org.springframework.util.StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    /**
     * 读取 classpath 文本。
     *
     * @param path 资源路径
     * @return 文本内容
     */
    private static String readClasspathText(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read DeepAgents prompt: " + path, ex);
        }
    }

    /**
     * 追加文本。
     *
     * @param sb 缓冲区
     * @param text 文本
     */
    private static void appendIfHasText(StringBuilder sb, String text) {
        if (!org.springframework.util.StringUtils.hasText(text)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("\n\n");
        }
        sb.append(text.trim());
    }
}
