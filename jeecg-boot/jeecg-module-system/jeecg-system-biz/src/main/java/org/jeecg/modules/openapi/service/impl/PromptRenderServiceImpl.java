package org.jeecg.modules.openapi.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.util.PromptRenderUtil;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.openapi.vo.PromptTemplateCodeVersionVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Prompt template rendering service implementation.
 */
@Slf4j
@Service
public class PromptRenderServiceImpl implements PromptRenderService {
    @Resource
    private IAiragPromptTemplateService airagPromptTemplateService;

    @Override
    public String renderPrompt(String code, String version, Map<String, String> variables) {
        return renderPromptSections(code, version, variables).getRenderedPrompt();
    }

    @Override
    public PromptRenderedSectionsVo renderPromptSections(String code, String version, Map<String, String> variables) {
        Map<String, String> safeVariables = PromptRenderUtil.safeVariables(variables);
        String safeCode = PromptRenderUtil.trimToEmpty(code);
        String safeVersion = PromptRenderUtil.trimToEmpty(version);

        AiragPromptTemplateVo template = airagPromptTemplateService.getTemplate(safeCode, safeVersion);
        String developerPrompt = PromptRenderUtil.trimToEmpty(template.getSections().get("developer_prompt"));
        String userPromptRaw = PromptRenderUtil.trimToEmpty(
                airagPromptTemplateService.renderSection(safeCode, safeVersion, "user_prompt_template", safeVariables)
        );
        String userPrompt = PromptRenderUtil.replaceUnfilledPlaceholders(userPromptRaw, "null");
        String outputSchemaHint = PromptRenderUtil.trimToEmpty(template.getSections().get("output_schema_hint"));
        String toolSchema = PromptRenderUtil.trimToEmpty(template.getSections().get("tool_schema"));
        String outputSpec = StringUtils.hasText(outputSchemaHint) ? outputSchemaHint : toolSchema;
        String renderedPrompt = PromptRenderUtil.buildFinalPrompt(developerPrompt, userPrompt, outputSpec);

        PromptRenderedSectionsVo sections = new PromptRenderedSectionsVo();
        sections.setTemplatePath(null);
        sections.setCode(safeCode);
        sections.setVersion(safeVersion);
        sections.setDeveloperPrompt(developerPrompt);
        sections.setUserPrompt(userPrompt);
        sections.setOutputSchemaHint(outputSchemaHint);
        sections.setToolSchema(toolSchema);
        sections.setRenderedPrompt(renderedPrompt);

        String logContent = renderedPrompt.length() > 300 ? renderedPrompt.substring(0, 300) + "...(truncated)" : renderedPrompt;
        log.info("Prompt template rendered code={} version={} vars={} rendered={}",
                safeCode, safeVersion, safeVariables, logContent);
        return sections;
    }

    @Override
    public String renderPrompt(String templatePath, Map<String, String> variables) {
        return renderPromptSections(templatePath, variables).getRenderedPrompt();
    }

    @Override
    public PromptRenderedSectionsVo renderPromptSections(String templatePath, Map<String, String> variables) {
        String normalizedPath = PromptRenderUtil.normalizeTemplatePath(templatePath);
        PromptTemplateCodeVersionVo codeVersion = PromptRenderUtil.parseCodeVersion(normalizedPath);
        PromptRenderedSectionsVo sections = renderPromptSections(codeVersion.getCode(), codeVersion.getVersion(), variables);
        sections.setTemplatePath(templatePath);
        return sections;
    }
}
