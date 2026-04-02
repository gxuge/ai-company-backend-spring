package org.jeecg.modules.airag.test;

import org.jeecg.modules.airag.prompts.service.impl.AiragPromptTemplateServiceImpl;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

    @Test
    void shouldLoadRoleTemplate() {
        AiragPromptTemplateServiceImpl service = new AiragPromptTemplateServiceImpl(new DefaultResourceLoader());
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
        AiragPromptTemplateServiceImpl service = new AiragPromptTemplateServiceImpl(new DefaultResourceLoader());
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
}
