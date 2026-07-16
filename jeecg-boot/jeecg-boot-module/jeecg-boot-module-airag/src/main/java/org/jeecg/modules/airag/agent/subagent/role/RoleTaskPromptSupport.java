package org.jeecg.modules.airag.agent.subagent.role;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.task.TaskAgentSupport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色子流程提示词变量助手。
 *
 * @author codex
 * @date 2026/7/11
 */
public final class RoleTaskPromptSupport {

    private static final String ATTR_ROLE_CORE_RESULT_JSON = "roleCoreResultJson";
    private static final String ATTR_ROLE_GENERATE_ROLE_RESULT_JSON = "roleGenerateRoleResultJson";
    private static final String ATTR_ROLE_IMAGE_RESULT_JSON = "roleImageResultJson";
    private static final String ATTR_ROLE_VOICE_RESULT_JSON = "roleVoiceResultJson";

    private RoleTaskPromptSupport() {
    }

    /**
     * 组装公共上下文变量。
     *
     * @param context 运行上下文
     * @return 变量集合
     */
    public static Map<String, String> baseVariables(AgentContext context) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("user_input", oConvertUtils.getString(context == null ? null : context.getUserInput()));
        variables.put("task_description", oConvertUtils.getString(context == null ? null : context.getAttribute("taskDescription")));
        return variables;
    }

    /**
     * 追加角色核心设定变量。
     *
     * @param variables 变量集合
     * @param context 运行上下文
     */
    public static void appendRoleCoreVariables(Map<String, String> variables, AgentContext context) {
        JSONObject core = resolveRoleCoreJson(context);
        if (core == null || core.isEmpty()) {
            return;
        }
        variables.putIfAbsent("role_core_result_json", core.toJSONString());
        variables.putIfAbsent("role_name", firstText(core, "roleName", "role_name"));
        variables.putIfAbsent("gender", firstText(core, "gender"));
        variables.putIfAbsent("occupation", firstText(core, "occupation"));
        variables.putIfAbsent("background_story", firstText(core, "backgroundStory", "background_story"));
        variables.putIfAbsent("greeting", firstText(core, "greeting"));
    }

    /**
     * 追加角色形象变量。
     *
     * @param variables 变量集合
     * @param context 运行上下文
     */
    public static void appendRoleImageVariables(Map<String, String> variables, AgentContext context) {
        JSONObject image = readJson(context, ATTR_ROLE_IMAGE_RESULT_JSON);
        if (image != null && !image.isEmpty()) {
            variables.putIfAbsent("role_image_result_json", image.toJSONString());
            variables.putIfAbsent("image_prompt", firstText(image, "imagePrompt", "renderedPrompt"));
            variables.putIfAbsent("image_url", firstText(image, "imageUrl"));
        }
        appendRoleCoreVariables(variables, context);
    }

    /**
     * 追加角色声音变量。
     *
     * @param variables 变量集合
     * @param context 运行上下文
     */
    public static void appendRoleVoiceVariables(Map<String, String> variables, AgentContext context) {
        JSONObject voice = readJson(context, ATTR_ROLE_VOICE_RESULT_JSON);
        if (voice != null && !voice.isEmpty()) {
            variables.putIfAbsent("role_voice_result_json", voice.toJSONString());
            variables.putIfAbsent("voice_name", firstText(voice, "voiceName", "preferredVoiceName"));
            variables.putIfAbsent("voice_preview_text", firstText(voice, "previewText"));
        }
        appendRoleCoreVariables(variables, context);
        appendRoleImageVariables(variables, context);
    }

    private static JSONObject resolveRoleCoreJson(AgentContext context) {
        JSONObject core = readJson(context, ATTR_ROLE_CORE_RESULT_JSON);
        if (core != null && !core.isEmpty()) {
            return core;
        }
        JSONObject generated = readJson(context, ATTR_ROLE_GENERATE_ROLE_RESULT_JSON);
        if (generated != null && !generated.isEmpty()) {
            JSONObject settingResult = generated.getJSONObject("settingResult");
            if (settingResult != null && !settingResult.isEmpty()) {
                return settingResult;
            }
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
