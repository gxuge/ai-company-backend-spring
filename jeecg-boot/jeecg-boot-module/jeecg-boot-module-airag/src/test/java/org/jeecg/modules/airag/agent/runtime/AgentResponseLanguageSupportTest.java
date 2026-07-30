package org.jeecg.modules.airag.agent.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentResponseLanguageSupportTest {

    @Test
    void shouldNormalizeSupportedLanguages() {
        Assertions.assertEquals("zh-CN", AgentResponseLanguageSupport.normalize("zh"));
        Assertions.assertEquals("zh-TW", AgentResponseLanguageSupport.normalize("zh-Hant-HK"));
        Assertions.assertEquals("en-US", AgentResponseLanguageSupport.normalize("en-GB"));
        Assertions.assertEquals("ja", AgentResponseLanguageSupport.normalize("ja-JP"));
        Assertions.assertEquals("ar", AgentResponseLanguageSupport.normalize("ar-SA"));
        Assertions.assertEquals("zh-CN", AgentResponseLanguageSupport.normalize("fr-FR"));
    }

    @Test
    void shouldStoreLanguageAndBuildPromptInstruction() {
        AgentContext context = new AgentContext();

        AgentResponseLanguageSupport.apply(context, "zh-TW");

        Assertions.assertEquals("zh-TW", context.getAttribute("responseLanguage"));
        Assertions.assertEquals("繁體中文", context.getAttribute("responseLanguageName"));
        Assertions.assertTrue(AgentResponseLanguageSupport.buildInstruction(context).contains("繁體中文（zh-TW）"));
    }

    @Test
    void shouldKeepLanguageWhenContextIsForked() {
        AgentContext context = new AgentContext();
        AgentResponseLanguageSupport.apply(context, "en-US");

        AgentContext child = context.fork("continue");

        Assertions.assertEquals("en-US", child.getAttribute("responseLanguage"));
        Assertions.assertEquals("英语", child.getAttribute("responseLanguageName"));
    }
}
