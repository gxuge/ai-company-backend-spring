package org.jeecg.modules.openapi.service;

import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;

import java.util.Map;

/**
 * Prompt template render service.
 * Input: template code/version + variables.
 * Output: rendered prompt text.
 */
public interface PromptRenderService {
    String renderPrompt(String code, String version, Map<String, String> variables);

    PromptRenderedSectionsVo renderPromptSections(String code, String version, Map<String, String> variables);

    /**
     * @deprecated prefer renderPrompt(code, version, variables)
     */
    @Deprecated
    String renderPrompt(String templatePath, Map<String, String> variables);

    /**
     * @deprecated prefer renderPromptSections(code, version, variables)
     */
    @Deprecated
    PromptRenderedSectionsVo renderPromptSections(String templatePath, Map<String, String> variables);
}
