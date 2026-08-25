package org.jeecg.modules.airag.test;

import org.jeecg.modules.airag.prompts.entity.AiragPrompts;
import org.jeecg.modules.airag.prompts.mapper.AiragPromptsMapper;
import org.jeecg.modules.airag.prompts.service.impl.AiragPromptTemplateServiceImpl;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * AIRAG classpath prompt 模板服务测试
 *
 * @author chenrui
 * @date 2026/3/31
 */
class AiragPromptTemplateServiceTest {

    private static final String ROLE_TEMPLATE_V1 = """
            TEMPLATE_BEGIN::role_generate::v1
            SECTION::meta
            code=role_generate
            version=v1
            output_mode=json

            SECTION::developer_prompt
            你是测试提示词生成器。

            SECTION::user_prompt_template
            role_direction={{role_direction}},gender={{gender}}

            SECTION::output_schema_hint
            {"type":"object"}
            TEMPLATE_END::role_generate::v1
            """;

    @Test
    void shouldLoadRoleTemplate() {
        AiragPromptTemplateServiceImpl service = buildServiceWithTemplate("role_generate", "v1", ROLE_TEMPLATE_V1);
        service.init();

        AiragPromptTemplateVo template = service.getTemplate("role_generate", "v1");
        Assertions.assertEquals("role_generate", template.getCode());
        Assertions.assertTrue(template.getSections().containsKey("meta"));
        Assertions.assertTrue(template.getSections().containsKey("developer_prompt"));
        Assertions.assertTrue(template.getSections().containsKey("user_prompt_template"));
        Assertions.assertTrue(template.getSections().containsKey("output_schema_hint"));
    }

    @Test
    void shouldRenderUserPromptSection() {
        AiragPromptTemplateServiceImpl service = buildServiceWithTemplate("role_generate", "v1", ROLE_TEMPLATE_V1);
        service.init();

        Map<String, String> variables = new HashMap<>();
        variables.put("role_direction", "温柔陪伴");
        variables.put("gender", "female");
        String rendered = service.renderSection("role_generate", "v1", "user_prompt_template", variables);

        Assertions.assertFalse(rendered.contains("{{role_direction}}"));
        Assertions.assertFalse(rendered.contains("{{gender}}"));
        Assertions.assertTrue(rendered.contains("温柔陪伴"));
        Assertions.assertTrue(rendered.contains("female"));
    }

    private AiragPromptTemplateServiceImpl buildServiceWithTemplate(String code, String version, String content) {
        AiragPromptsMapper promptsMapper = Mockito.mock(AiragPromptsMapper.class);
        AiragPrompts prompts = new AiragPrompts();
        prompts.setPromptKey(code);
        prompts.setVersion(version);
        prompts.setContent(content);
        Mockito.when(promptsMapper.selectOne(Mockito.any(), Mockito.eq(false))).thenReturn(prompts);
        return new AiragPromptTemplateServiceImpl(new DefaultResourceLoader(), promptsMapper);
    }
}
