package org.jeecg.modules.system.agent.task;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import com.alibaba.fastjson.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务 Agent 通用辅助工具。
 *
 * @author codex
 * @date 2026/7/1
 */
public final class TaskAgentSupport {

    /**
     * 任务 Agent 的最低输入长度阈值。
     */
    private static final int VAGUE_INPUT_MAX_LENGTH = 12;

    private TaskAgentSupport() {
    }

    /**
     * 从上下文构造最小登录用户对象。
     *
     * @param context 运行上下文
     * @return 登录用户
     */
    public static LoginUser buildLoginUser(AgentContext context) {
        String userId = normalizeText(context == null ? null : context.getUserId());
        if (!oConvertUtils.isNotEmpty(userId)) {
            throw new IllegalStateException("任务 Agent 缺少用户信息");
        }
        return new LoginUser()
                .setId(userId)
                .setUsername(userId)
                .setRealname(userId);
    }

    /**
     * 归一化文本。
     *
     * @param value 原始值
     * @return 归一化结果
     */
    public static String normalizeText(String value) {
        if (!oConvertUtils.isNotEmpty(value)) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 判断是否为较泛的角色请求。
     *
     * @param userInput 用户输入
     * @return 是否适合先走 preset
     */
    public static boolean isVagueRoleRequest(String userInput) {
        String text = normalizeText(userInput);
        if (!oConvertUtils.isNotEmpty(text)) {
            return true;
        }
        if (text.length() <= VAGUE_INPUT_MAX_LENGTH) {
            return true;
        }
        boolean hasSpecificHints = containsAny(text,
                "性别", "职业", "背景", "人设", "设定", "头像", "声音", "外貌", "气质", "服装", "性格");
        return !hasSpecificHints;
    }

    /**
     * 判断是否为较泛的故事请求。
     *
     * @param userInput 用户输入
     * @return 是否适合先走 preset
     */
    public static boolean isVagueStoryRequest(String userInput) {
        String text = normalizeText(userInput);
        if (!oConvertUtils.isNotEmpty(text)) {
            return true;
        }
        if (text.length() <= VAGUE_INPUT_MAX_LENGTH) {
            return true;
        }
        boolean hasSpecificHints = containsAny(text,
                "标题", "简介", "设定", "场景", "大纲", "章节", "背景", "时间", "地点", "冲突", "开头", "续写");
        return !hasSpecificHints;
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param text 文本
     * @param keywords 关键词
     * @return 是否包含
     */
    public static boolean containsAny(String text, String... keywords) {
        if (!oConvertUtils.isNotEmpty(text) || keywords == null || keywords.length == 0) {
            return false;
        }
        for (String keyword : keywords) {
            if (oConvertUtils.isNotEmpty(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取上下文中的 Map 型属性。
     *
     * @param context 运行上下文
     * @param key 属性键
     * @return Map，若不存在则返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> readMapAttribute(AgentContext context, String key) {
        if (context == null || !oConvertUtils.isNotEmpty(key)) {
            return new LinkedHashMap<>();
        }
        Object value = context.getAttribute(key);
        if (!(value instanceof Map<?, ?> rawMap)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return map;
    }

    /**
     * 读取上下文中的 JSON 属性。
     *
     * @param context 运行上下文
     * @param key 属性键
     * @return JSONObject，若不存在则返回空对象
     */
    public static JSONObject readJsonAttribute(AgentContext context, String key) {
        if (context == null || !oConvertUtils.isNotEmpty(key)) {
            return new JSONObject();
        }
        Object value = context.getAttribute(key);
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof String text && oConvertUtils.isNotEmpty(text)) {
            try {
                JSONObject parsed = JSONObject.parseObject(text);
                return parsed == null ? new JSONObject() : parsed;
            } catch (Exception ignored) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    /**
     * 读取上下文中的字符串属性。
     *
     * @param context 运行上下文
     * @param key 属性键
     * @return 字符串
     */
    public static String readStringAttribute(AgentContext context, String key) {
        if (context == null || !oConvertUtils.isNotEmpty(key)) {
            return null;
        }
        Object value = context.getAttribute(key);
        return value == null ? null : normalizeText(String.valueOf(value));
    }
}
