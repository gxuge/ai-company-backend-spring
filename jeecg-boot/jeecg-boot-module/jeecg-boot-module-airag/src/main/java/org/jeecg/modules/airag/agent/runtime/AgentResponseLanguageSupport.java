package org.jeecg.modules.airag.agent.runtime;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Agent 回复语言支持。
 *
 * @author codex
 * @date 2026/7/30
 */
public final class AgentResponseLanguageSupport {
    public static final String ATTR_RESPONSE_LANGUAGE = "responseLanguage";
    public static final String ATTR_RESPONSE_LANGUAGE_NAME = "responseLanguageName";
    public static final String DEFAULT_LANGUAGE = "zh-CN";

    private AgentResponseLanguageSupport() {
    }

    /**
     * 将请求语言写入 Agent 上下文。
     *
     * @param context Agent 上下文
     * @param languageTag 请求语言标签
     */
    public static void apply(AgentContext context, String languageTag) {
        if (context == null) {
            return;
        }
        String language = normalize(languageTag);
        context.putAttribute(ATTR_RESPONSE_LANGUAGE, language);
        context.putAttribute(ATTR_RESPONSE_LANGUAGE_NAME, displayName(language));
    }

    /**
     * 归一化前端支持的语言标签。
     *
     * @param languageTag 原始语言标签
     * @return 标准语言标签
     */
    public static String normalize(String languageTag) {
        if (!StringUtils.hasText(languageTag)) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = languageTag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (normalized.startsWith("zh-hant")
                || normalized.startsWith("zh-tw")
                || normalized.startsWith("zh-hk")
                || normalized.startsWith("zh-mo")) {
            return "zh-TW";
        }
        if (normalized.startsWith("zh")) {
            return DEFAULT_LANGUAGE;
        }
        if (normalized.startsWith("en")) {
            return "en-US";
        }
        if (normalized.startsWith("ja")) {
            return "ja";
        }
        if (normalized.startsWith("ar")) {
            return "ar";
        }
        return DEFAULT_LANGUAGE;
    }

    /**
     * 构造本轮动态语言要求。
     *
     * @param context Agent 上下文
     * @return 可注入系统消息的语言要求
     */
    public static String buildInstruction(AgentContext context) {
        String language = normalize(context == null
                ? null
                : String.valueOf(context.getAttribute(ATTR_RESPONSE_LANGUAGE)));
        return """
                # 当前回复语言

                * 语言：%s（%s）
                * 本轮所有面向对方的自然语言回复必须使用该语言。
                """.formatted(displayName(language), language).trim();
    }

    /**
     * 获取语言的中文显示名称。
     *
     * @param language 标准语言标签
     * @return 显示名称
     */
    public static String displayName(String language) {
        return switch (normalize(language)) {
            case "zh-TW" -> "繁體中文";
            case "en-US" -> "英语";
            case "ja" -> "日语";
            case "ar" -> "阿拉伯语";
            default -> "简体中文";
        };
    }
}
