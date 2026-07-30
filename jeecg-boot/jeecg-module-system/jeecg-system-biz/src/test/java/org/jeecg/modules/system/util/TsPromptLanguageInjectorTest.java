package org.jeecg.modules.system.util;

import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TsPromptLanguageInjectorTest {

    @AfterEach
    void resetLocaleContext() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldInjectJapaneseWithoutChangingSchema() {
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        PromptRenderedSectionsVo sections = new PromptRenderedSectionsVo();
        sections.setDeveloperPrompt("你是角色生成助手。");
        sections.setUserPrompt("生成一个角色");
        sections.setToolSchema("{\"type\":\"object\"}");

        TsPromptLanguageInjector.inject(sections);

        assertTrue(sections.getDeveloperPrompt().contains("必须使用日本語回复"));
        assertTrue(sections.getRenderedPrompt().contains("必须使用日本語回复"));
        assertEquals("{\"type\":\"object\"}", sections.getToolSchema());
    }

    @Test
    void shouldInjectTraditionalChineseIntoPlainPrompt() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("zh-TW"));

        String prompt = TsPromptLanguageInjector.inject("请生成故事。");

        assertTrue(prompt.contains("必须使用繁體中文回复"));
        assertTrue(prompt.contains("JSON字段名、枚举值、Schema字段和技术标识保持原样"));
    }
}
