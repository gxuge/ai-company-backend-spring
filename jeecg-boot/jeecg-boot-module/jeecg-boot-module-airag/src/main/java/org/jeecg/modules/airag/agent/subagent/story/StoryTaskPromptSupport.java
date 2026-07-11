package org.jeecg.modules.airag.agent.subagent.story;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.task.TaskAgentSupport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事子流程提示词变量助手。
 *
 * <p>负责把上下文里的通用字段、核心设定和背景信息整理成 LLM 可直接使用的变量。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
public final class StoryTaskPromptSupport {

    private static final String ATTR_STORY_CORE_RESULT_JSON = "storyCoreResultJson";
    private static final String ATTR_STORY_CORE_PRESET_RESULT_JSON = "storyCorePresetResultJson";
    private static final String ATTR_STORY_FULL_GENERATE_RESULT_JSON = "storyFullGenerateResultJson";
    private static final String ATTR_STORY_SCENE_RESULT_JSON = "storySceneResultJson";
    private static final String ATTR_STORY_BACKGROUND_RESULT_JSON = "storyBackgroundResultJson";

    private StoryTaskPromptSupport() {
    }

    /**
     * 组装公共变量。
     *
     * @param context 运行上下文
     * @return 变量集合
     */
    public static Map<String, String> baseVariables(AgentContext context) {
        Map<String, String> variables = new LinkedHashMap<>();
        Map<?, ?> promptVariables = context == null ? null : context.getAttribute("promptVariables", Map.class);
        if (promptVariables != null) {
            for (Map.Entry<?, ?> entry : promptVariables.entrySet()) {
                if (entry.getKey() != null) {
                    variables.put(String.valueOf(entry.getKey()), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                }
            }
        }
        variables.putIfAbsent("user_input", oConvertUtils.getString(context == null ? null : context.getUserInput()));
        variables.putIfAbsent("session_summary", oConvertUtils.getString(context == null ? null : context.getAttribute("sessionSummary")));
        variables.putIfAbsent("recent_messages_block", oConvertUtils.getString(context == null ? null : context.getAttribute("recentMessagesBlock")));
        variables.putIfAbsent("confirmed_fields_json", oConvertUtils.getString(context == null ? null : context.getAttribute("confirmedFieldsJson")));
        variables.putIfAbsent("missing_fields_json", oConvertUtils.getString(context == null ? null : context.getAttribute("missingFieldsJson")));
        return variables;
    }

    /**
     * 追加故事核心变量。
     *
     * @param variables 变量集合
     * @param context 运行上下文
     */
    public static void appendStoryCoreVariables(Map<String, String> variables, AgentContext context) {
        JSONObject core = resolveStoryCoreJson(context);
        if (core == null || core.isEmpty()) {
            return;
        }
        variables.putIfAbsent("story_core_result_json", core.toJSONString());
        variables.putIfAbsent("title", firstText(core, "title"));
        variables.putIfAbsent("story_mode", firstText(core, "storyMode", "story_mode"));
        variables.putIfAbsent("story_intro", firstText(core, "storyIntro", "story_intro"));
        variables.putIfAbsent("story_setting", firstText(core, "storySetting", "story_setting"));
        variables.putIfAbsent("site_setting", firstText(core, "siteSetting", "site_setting"));
        variables.putIfAbsent("plot_outline", firstText(core, "plotOutline", "plot_outline"));
    }

    /**
     * 追加故事背景 / 场景变量。
     *
     * @param variables 变量集合
     * @param context 运行上下文
     */
    public static void appendStoryBackgroundVariables(Map<String, String> variables, AgentContext context) {
        JSONObject background = resolveStoryBackgroundJson(context);
        if (background != null && !background.isEmpty()) {
            variables.putIfAbsent("story_background_result_json", background.toJSONString());
            variables.putIfAbsent("story_scene_result_json", background.toJSONString());
            variables.putIfAbsent("story_background", firstText(background, "storyBackground", "story_background", "sceneSummary", "scene_summary"));
            variables.putIfAbsent("scene_setting", firstText(background, "sceneSetting", "scene_setting"));
            variables.putIfAbsent("scene_name_snapshot", firstText(background, "sceneNameSnapshot", "scene_name_snapshot"));
            variables.putIfAbsent("scene_summary", firstText(background, "sceneSummary", "scene_summary"));
            variables.putIfAbsent("scene_elements_json", firstText(background, "sceneElements", "scene_elements"));
        }
        appendStoryCoreVariables(variables, context);
    }

    /**
     * 判断是否是确认类输入。
     *
     * @param value 用户输入
     * @return 是否确认
     */
    public static boolean isConfirmation(String value) {
        String text = TaskAgentSupport.normalizeText(value);
        if (!oConvertUtils.isNotEmpty(text)) {
            return false;
        }
        return text.contains("确认")
                || text.contains("可以")
                || text.contains("就这样")
                || text.contains("没问题")
                || text.contains("继续")
                || text.contains("好的")
                || "好".equals(text)
                || "行".equals(text)
                || "对".equals(text)
                || text.equalsIgnoreCase("yes")
                || text.equalsIgnoreCase("ok");
    }

    private static JSONObject resolveStoryCoreJson(AgentContext context) {
        JSONObject core = readJson(context, ATTR_STORY_CORE_RESULT_JSON);
        if (core != null && !core.isEmpty()) {
            return core;
        }
        JSONObject preset = readJson(context, ATTR_STORY_CORE_PRESET_RESULT_JSON);
        if (preset != null && !preset.isEmpty()) {
            return preset;
        }
        JSONObject full = readJson(context, ATTR_STORY_FULL_GENERATE_RESULT_JSON);
        if (full != null && !full.isEmpty()) {
            return full;
        }
        return null;
    }

    private static JSONObject resolveStoryBackgroundJson(AgentContext context) {
        JSONObject scene = readJson(context, ATTR_STORY_SCENE_RESULT_JSON);
        if (scene != null && !scene.isEmpty()) {
            return scene;
        }
        JSONObject background = readJson(context, ATTR_STORY_BACKGROUND_RESULT_JSON);
        if (background != null && !background.isEmpty()) {
            return background;
        }
        return null;
    }

    private static JSONObject readJson(AgentContext context, String key) {
        JSONObject value = TaskAgentSupport.readJsonAttribute(context, key);
        return value == null ? new JSONObject() : value;
    }

    private static String firstText(JSONObject source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (!oConvertUtils.isNotEmpty(key)) {
                continue;
            }
            String value = source.getString(key);
            if (oConvertUtils.isNotEmpty(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
