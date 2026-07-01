package org.jeecg.modules.system.agent.task;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片任务通用辅助工具。
 *
 * @author codex
 * @date 2026/7/1
 */
public final class TaskImageSupport {

    private TaskImageSupport() {
    }

    /**
     * 将上下文中的 promptVariables 读取为字符串 Map。
     *
     * @param context 运行上下文
     * @return prompt 变量
     */
    public static Map<String, String> readPromptVariables(org.jeecg.modules.airag.agent.runtime.AgentContext context) {
        Map<String, Object> rawMap = TaskAgentSupport.readMapAttribute(context, "promptVariables");
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 从上下文中读取会话记忆。
     *
     * @param context 运行上下文
     * @return 记忆对象
     */
    public static JSONObject readSessionMemory(org.jeecg.modules.airag.agent.runtime.AgentContext context) {
        return TaskAgentSupport.readJsonAttribute(context, "sessionMemoryJson");
    }

    /**
     * 从多源里读取文本。
     *
     * @param promptVariables prompt 变量
     * @param memory 会话记忆
     * @param fallback 兜底值
     * @param keys 候选键
     * @return 文本
     */
    public static String resolveText(Map<String, String> promptVariables, JSONObject memory, String fallback, String... keys) {
        if (keys != null) {
            for (String key : keys) {
                String value = lookup(promptVariables, memory, key);
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        String normalizedFallback = PromptRuntimeUtil.trimToNull(fallback);
        return normalizedFallback;
    }

    /**
     * 从多源里读取 Long。
     *
     * @param promptVariables prompt 变量
     * @param memory 会话记忆
     * @param keys 候选键
     * @return Long
     */
    public static Long resolveLong(Map<String, String> promptVariables, JSONObject memory, String... keys) {
        if (keys == null || keys.length == 0) {
            return null;
        }
        for (String key : keys) {
            String value = lookup(promptVariables, memory, key);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (Exception ignored) {
                // ignore
            }
        }
        return null;
    }

    /**
     * 拼接故事场景图提示词。
     *
     * @param title 标题
     * @param storyIntro 故事简介
     * @param storySetting 故事设定
     * @param siteSetting 场景设定
     * @param plotOutline 剧情大纲
     * @param styleName 风格
     * @param aspectRatio 比例
     * @param referenceImageUrl 参考图
     * @param userInput 用户输入
     * @param taskGoal 任务目标
     * @return 提示词
     */
    public static String buildStorySceneImagePrompt(String title,
                                                    String storyIntro,
                                                    String storySetting,
                                                    String siteSetting,
                                                    String plotOutline,
                                                    String styleName,
                                                    String aspectRatio,
                                                    String referenceImageUrl,
                                                    String userInput,
                                                    String taskGoal) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "故事场景图需求", "请生成一张适合互动叙事场景的故事背景图，突出氛围、空间感和可视化线索");
        appendLine(builder, "标题", title);
        appendLine(builder, "故事简介", storyIntro);
        appendLine(builder, "故事设定", storySetting);
        appendLine(builder, "场景设定", siteSetting);
        appendLine(builder, "剧情大纲", plotOutline);
        appendLine(builder, "用户需求", PromptRuntimeUtil.firstNonBlank(taskGoal, userInput));
        appendLine(builder, "风格提示", styleName);
        appendLine(builder, "画面比例", aspectRatio);
        appendLine(builder, "参考图", referenceImageUrl);
        appendLine(builder, "画面要求", "画面要有明确主体、环境层次、情绪氛围与故事感，不要只做抽象背景");
        return builder.toString();
    }

    /**
     * 拼接角色图片提示词。
     *
     * @param roleName 角色名称
     * @param gender 性别
     * @param occupation 职业
     * @param backgroundStory 背景故事
     * @param styleName 风格
     * @param aspectRatio 比例
     * @param referenceImageUrl 参考图
     * @param userInput 用户输入
     * @param taskGoal 任务目标
     * @return 提示词
     */
    public static String buildRoleImagePrompt(String roleName,
                                              String gender,
                                              String occupation,
                                              String backgroundStory,
                                              String styleName,
                                              String aspectRatio,
                                              String referenceImageUrl,
                                              String userInput,
                                              String taskGoal) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "角色图片需求", "请生成一张清晰的角色形象图，突出人物辨识度、气质和职业感");
        appendLine(builder, "角色名称", roleName);
        appendLine(builder, "性别", gender);
        appendLine(builder, "职业", occupation);
        appendLine(builder, "背景故事", backgroundStory);
        appendLine(builder, "用户需求", PromptRuntimeUtil.firstNonBlank(taskGoal, userInput));
        appendLine(builder, "风格提示", styleName);
        appendLine(builder, "画面比例", aspectRatio);
        appendLine(builder, "参考图", referenceImageUrl);
        appendLine(builder, "画面要求", "主体清晰、角色特征明确、服装与身份一致、背景不要喧宾夺主");
        return builder.toString();
    }

    /**
     * 提取首个图片地址。
     *
     * @param response 图片响应
     * @return 图片地址
     */
    public static String extractFirstImageUrl(MiniMaxImageResponseVo response) {
        if (response == null) {
            return null;
        }
        List<String> urls = response.getImageUrls();
        if (urls != null) {
            for (String url : urls) {
                if (StringUtils.hasText(url)) {
                    return url.trim();
                }
            }
        }
        List<String> originalUrls = response.getOriginalImageUrls();
        if (originalUrls != null) {
            for (String url : originalUrls) {
                if (StringUtils.hasText(url)) {
                    return url.trim();
                }
            }
        }
        return null;
    }

    /**
     * 读取候选键的首个有效值。
     */
    private static String lookup(Map<String, String> promptVariables, JSONObject memory, String key) {
        if (oConvertUtils.isNotEmpty(key)) {
            if (promptVariables != null) {
                String value = PromptRuntimeUtil.trimToNull(promptVariables.get(key));
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
            if (memory != null) {
                String value = PromptRuntimeUtil.trimToNull(memory.getString(key));
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * 添加单行文本。
     */
    private static void appendLine(StringBuilder builder, String label, String value) {
        String text = PromptRuntimeUtil.trimToNull(value);
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append("：").append(text.trim());
    }
}
