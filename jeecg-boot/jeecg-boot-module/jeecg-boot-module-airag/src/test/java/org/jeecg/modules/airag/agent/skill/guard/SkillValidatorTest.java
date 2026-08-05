package org.jeecg.modules.airag.agent.skill.guard;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SkillValidatorTest {

    private final SkillValidator validator = new SkillValidator();

    @Test
    void shouldAllowUnrestrictedDisplayName() {
        SkillDefinition definition = validDefinition();
        definition.setName("角色形象生成 / Role Image (正式版) 🎨");

        Assertions.assertDoesNotThrow(() -> this.validator.validateDefinition(definition));
    }

    @Test
    void shouldStillRequireDisplayName() {
        SkillDefinition definition = validDefinition();
        definition.setName(" ");

        Assertions.assertThrows(
                JeecgBootException.class,
                () -> this.validator.validateDefinition(definition)
        );
    }

    @Test
    void shouldStillRejectInvalidCode() {
        SkillDefinition definition = validDefinition();
        definition.setCode("角色形象生成");

        Assertions.assertThrows(
                JeecgBootException.class,
                () -> this.validator.validateDefinition(definition)
        );
    }

    private SkillDefinition validDefinition() {
        SkillDefinition definition = new SkillDefinition();
        definition.setCode("role_create_image");
        definition.setName("角色形象生成");
        definition.setDescription("生成角色形象图片。");
        definition.setDomain("role");
        definition.setVersion("1.0.0");
        return definition;
    }
}
