package org.jeecg.modules.system.util;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * TS 接口回复语言支持。
 */
public final class TsResponseLanguageSupport {
    private static final String DEFAULT_LANGUAGE = "zh-CN";

    private TsResponseLanguageSupport() {
    }

    public static String currentLanguageTag() {
        Locale locale = LocaleContextHolder.getLocale();
        return normalize(locale == null ? null : locale.toLanguageTag());
    }

    public static String currentLanguageName() {
        return switch (currentLanguageTag()) {
            case "zh-TW" -> "繁體中文";
            case "en-US" -> "English";
            case "ja" -> "日本語";
            case "ar" -> "العربية";
            default -> "简体中文";
        };
    }

    public static String text(String simplifiedChinese,
                              String traditionalChinese,
                              String english,
                              String japanese,
                              String arabic) {
        return switch (currentLanguageTag()) {
            case "zh-TW" -> traditionalChinese;
            case "en-US" -> english;
            case "ja" -> japanese;
            case "ar" -> arabic;
            default -> simplifiedChinese;
        };
    }

    private static String normalize(String languageTag) {
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
}
