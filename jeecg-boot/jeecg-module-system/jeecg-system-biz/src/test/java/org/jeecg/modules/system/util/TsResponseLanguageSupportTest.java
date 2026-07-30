package org.jeecg.modules.system.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TsResponseLanguageSupportTest {

    @AfterEach
    void resetLocaleContext() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldReturnJapaneseText() {
        LocaleContextHolder.setLocale(Locale.JAPANESE);

        String text = TsResponseLanguageSupport.text(
                "简体", "繁體", "English", "日本語", "العربية");

        assertEquals("日本語", text);
        assertEquals("日本語", TsResponseLanguageSupport.currentLanguageName());
    }

    @Test
    void shouldFallbackToSimplifiedChineseForUnsupportedLanguage() {
        LocaleContextHolder.setLocale(Locale.FRENCH);

        String text = TsResponseLanguageSupport.text(
                "简体", "繁體", "English", "日本語", "العربية");

        assertEquals("简体", text);
        assertEquals("zh-CN", TsResponseLanguageSupport.currentLanguageTag());
    }
}
