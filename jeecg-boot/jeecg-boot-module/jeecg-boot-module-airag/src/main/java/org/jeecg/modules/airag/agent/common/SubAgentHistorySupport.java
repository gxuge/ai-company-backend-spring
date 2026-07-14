package org.jeecg.modules.airag.agent.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 子 Agent 历史记忆工具。
 *
 * <p>用于在会话级 JSON 中保存每个子 Agent 最近的执行记录，
 * 并在父 Agent 路由到具体子 Agent 时提取对应历史片段。</p>
 *
 * @author codex
 * @date 2026/7/3
 */
public final class SubAgentHistorySupport {

    /**
     * 单个子 Agent 默认保留的历史条数。
     */
    public static final int DEFAULT_HISTORY_LIMIT = 2;

    private SubAgentHistorySupport() {
    }

    /**
     * 选取指定子 Agent 的历史记录数组，并转为 JSON 文本。
     *
     * @param historyJson 会话级历史 JSON
     * @param subAgentName 子 Agent 名称
     * @return 子 Agent 历史 JSON 数组文本
     */
    public static String selectHistoryJson(String historyJson, String subAgentName) {
        return selectHistoryArray(historyJson, subAgentName).toJSONString();
    }

    /**
     * 选取指定子 Agent 的历史记录数组。
     *
     * @param historyJson 会话级历史 JSON
     * @param subAgentName 子 Agent 名称
     * @return 历史数组
     */
    public static JSONArray selectHistoryArray(String historyJson, String subAgentName) {
        String normalizedSubAgentName = normalizeText(subAgentName);
        if (!StringUtils.hasText(normalizedSubAgentName)) {
            return new JSONArray();
        }
        JSONObject root = parseRoot(historyJson);
        if (root.isEmpty()) {
            return new JSONArray();
        }
        Object value = root.get(normalizedSubAgentName);
        if (value instanceof JSONArray array) {
            return array;
        }
        if (value instanceof List<?> list) {
            JSONArray array = new JSONArray();
            array.addAll(list);
            return array;
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                JSONArray array = JSONArray.parseArray(text);
                return array == null ? new JSONArray() : array;
            } catch (Exception ignore) {
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

    /**
     * 追加一条子 Agent 历史记录，并裁剪到指定长度。
     *
     * @param historyJson 原始历史 JSON
     * @param subAgentName 子 Agent 名称
     * @param record 记录内容
     * @param limit 保留条数
     * @return 新的历史 JSON
     */
    public static String appendHistoryJson(String historyJson, String subAgentName, JSONObject record, int limit) {
        String normalizedSubAgentName = normalizeText(subAgentName);
        if (!StringUtils.hasText(normalizedSubAgentName)) {
            return normalizeJson(historyJson);
        }
        JSONObject root = parseRoot(historyJson);
        JSONArray history = selectHistoryArray(historyJson, normalizedSubAgentName);
        if (record != null) {
            history.add(record);
        }
        if (limit > 0 && history.size() > limit) {
            int fromIndex = Math.max(0, history.size() - limit);
            JSONArray trimmed = new JSONArray();
            for (int i = fromIndex; i < history.size(); i++) {
                trimmed.add(history.get(i));
            }
            history = trimmed;
        }
        root.put(normalizedSubAgentName, history);
        return root.toJSONString();
    }

    /**
     * 统计历史条数。
     */
    public static int countHistory(String historyJson) {
        JSONObject root = parseRoot(historyJson);
        return root.values().stream()
                .mapToInt(value -> {
                    if (value instanceof JSONArray array) {
                        return array.size();
                    }
                    if (value instanceof List<?> list) {
                        return list.size();
                    }
                    return 0;
                })
                .sum();
    }

    /**
     * 获取最新一条历史。
     */
    public static JSONObject latestRecord(String historyJson) {
        JSONArray array = parseFirstArray(historyJson);
        if (array.isEmpty()) {
            return new JSONObject();
        }
        Object value = array.get(array.size() - 1);
        if (value instanceof JSONObject object) {
            return object;
        }
        try {
            return JSONObject.parseObject(String.valueOf(value));
        } catch (Exception ignore) {
            return new JSONObject();
        }
    }

    private static JSONObject parseRoot(String historyJson) {
        if (!StringUtils.hasText(historyJson)) {
            return new JSONObject();
        }
        try {
            JSONObject parsed = JSONObject.parseObject(historyJson);
            return parsed == null ? new JSONObject() : parsed;
        } catch (Exception ignore) {
            return new JSONObject();
        }
    }

    private static JSONArray parseFirstArray(String historyJson) {
        if (StringUtils.hasText(historyJson) && historyJson.trim().startsWith("[")) {
            try {
                JSONArray array = JSONArray.parseArray(historyJson);
                return array == null ? new JSONArray() : array;
            } catch (Exception ignore) {
                return new JSONArray();
            }
        }
        JSONObject root = parseRoot(historyJson);
        if (root.isEmpty()) {
            return new JSONArray();
        }
        Object firstValue = root.values().stream().findFirst().orElse(null);
        if (firstValue instanceof JSONArray array) {
            return array;
        }
        if (firstValue instanceof List<?> list) {
            JSONArray array = new JSONArray();
            array.addAll(list);
            return array;
        }
        return new JSONArray();
    }

    private static String normalizeJson(String historyJson) {
        JSONObject root = parseRoot(historyJson);
        return root.toJSONString();
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
