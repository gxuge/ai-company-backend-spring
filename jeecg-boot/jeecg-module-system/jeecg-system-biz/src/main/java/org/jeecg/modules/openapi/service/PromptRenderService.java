package org.jeecg.modules.openapi.service;

import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;

import java.util.Map;

/**
 * Prompt template render service.
 * Input: template path + variables.
 * Output: rendered prompt text.
 */
public interface PromptRenderService {
    String renderPrompt(String templatePath, Map<String, String> variables);

    PromptRenderedSectionsVo renderPromptSections(String templatePath, Map<String, String> variables);
}
