package org.jeecg.modules.airag.agent.task;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;
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
            throw new AgentErrorException(AgentErrorCode.RUNTIME_USER_CONTEXT_MISSING);
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
