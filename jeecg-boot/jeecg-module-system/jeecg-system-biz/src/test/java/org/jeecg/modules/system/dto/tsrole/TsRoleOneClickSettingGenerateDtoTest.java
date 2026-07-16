package org.jeecg.modules.system.dto.tsrole;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TsRoleOneClickSettingGenerateDtoTest {

    @Test
    void shouldRecognizeGreetingOptimizeMode() {
        TsRoleOneClickSettingGenerateDto dto = new TsRoleOneClickSettingGenerateDto();
        dto.setTemplateMode(" greeting_optimize ");

        dto.normalize();

        assertEquals(TsRoleOneClickSettingGenerateDto.TEMPLATE_MODE_GREETING_OPTIMIZE, dto.getTemplateMode());
        assertTrue(dto.isGreetingOptimizeMode());
        assertFalse(dto.isBackgroundOptimizeMode());
    }

    @Test
    void shouldFallbackUnknownTemplateModeToCore() {
        TsRoleOneClickSettingGenerateDto dto = new TsRoleOneClickSettingGenerateDto();
        dto.setTemplateMode("intro_optimize");

        dto.normalize();

        assertEquals(TsRoleOneClickSettingGenerateDto.TEMPLATE_MODE_CORE, dto.getTemplateMode());
        assertFalse(dto.isGreetingOptimizeMode());
    }
}
