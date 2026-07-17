package org.jeecg.modules.airag.agent.interaction;

import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户待交互状态支持。
 *
 * <p>统一保存 Tool 发起的确认或选项交互，并校验前端回传的交互标识和选项值。</p>
 *
 * @author codex
 * @date 2026/7/17
 */
public final class UserInteractionSupport {
    public static final String ATTR_PENDING_INTERACTION = "pendingUserInteraction";
    public static final String ATTR_INTERACTION_ID = "interactionId";
    public static final String ATTR_OPTION_VALUE = "optionValue";

    private UserInteractionSupport() {
    }

    /**
     * 创建并保存一条待用户处理的交互。
     */
    public static Map<String, Object> createPending(AgentContext context,
                                                    String interactionType,
                                                    String toolName,
                                                    String sourceNode,
                                                    String resumeNode,
                                                    String question,
                                                    String summary,
                                                    String contextRef,
                                                    List<Map<String, String>> options) {
        Map<String, Object> interaction = new LinkedHashMap<>();
        interaction.put("interactionId", UUIDGenerator.generate());
        interaction.put("interactionType", normalize(interactionType, "options"));
        interaction.put("toolName", normalize(toolName, null));
        interaction.put("sourceNode", normalize(sourceNode, null));
        interaction.put("resumeNode", normalize(resumeNode, null));
        interaction.put("question", normalize(question, null));
        interaction.put("summary", normalize(summary, null));
        interaction.put("contextRef", normalize(contextRef, null));
        interaction.put("options", copyOptions(options));
        interaction.put("status", "WAITING_USER");
        interaction.put("suspendRun", true);
        interaction.put("createdAt", System.currentTimeMillis());
        if (context != null) {
            context.putAttribute(ATTR_PENDING_INTERACTION, interaction);
        }
        return interaction;
    }

    /**
     * 获取当前待交互数据的副本。
     */
    public static Map<String, Object> getPending(AgentContext context) {
        if (context == null) {
            return new LinkedHashMap<>();
        }
        Object raw = context.getAttribute(ATTR_PENDING_INTERACTION);
        if (!(raw instanceof Map<?, ?> rawMap)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> pending = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null) {
                pending.put(String.valueOf(key), value);
            }
        });
        return pending;
    }

    /**
     * 判断当前是否存在待交互。
     */
    public static boolean hasPending(AgentContext context) {
        return !getPending(context).isEmpty();
    }

    /**
     * 读取并校验前端选择值。
     *
     * @return 未选择时返回 null
     */
    public static String resolveSelectedValue(AgentContext context, Map<String, Object> pending) {
        if (context == null || pending == null || pending.isEmpty()) {
            return null;
        }
        String optionValue = normalize(oConvertUtils.getString(context.getAttribute(ATTR_OPTION_VALUE)), null);
        if (optionValue == null) {
            return null;
        }
        String expectedInteractionId = normalize(oConvertUtils.getString(pending.get("interactionId")), null);
        String actualInteractionId = normalize(oConvertUtils.getString(context.getAttribute(ATTR_INTERACTION_ID)), null);
        if (actualInteractionId != null
                && expectedInteractionId != null
                && !expectedInteractionId.equals(actualInteractionId)) {
            throw new IllegalArgumentException("当前选项已失效，请重新选择");
        }
        if (!containsOption(pending.get("options"), optionValue)) {
            throw new IllegalArgumentException("不支持的选项值：" + optionValue);
        }
        return optionValue;
    }

    /**
     * 清理已完成的待交互和本轮选择值。
     */
    public static void clear(AgentContext context) {
        if (context == null) {
            return;
        }
        context.removeAttribute(ATTR_PENDING_INTERACTION);
        context.removeAttribute(ATTR_INTERACTION_ID);
        context.removeAttribute(ATTR_OPTION_VALUE);
    }

    private static boolean containsOption(Object rawOptions, String optionValue) {
        if (!(rawOptions instanceof Iterable<?> iterable)) {
            return false;
        }
        for (Object item : iterable) {
            if (!(item instanceof Map<?, ?> option)) {
                continue;
            }
            String value = normalize(oConvertUtils.getString(option.get("value")), null);
            if (value == null) {
                value = normalize(oConvertUtils.getString(option.get("optionValue")), null);
            }
            if (optionValue.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<String, String>> copyOptions(List<Map<String, String>> options) {
        List<Map<String, String>> copied = new ArrayList<>();
        if (options == null) {
            return copied;
        }
        for (Map<String, String> option : options) {
            if (option != null && !option.isEmpty()) {
                copied.add(new LinkedHashMap<>(option));
            }
        }
        return copied;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
