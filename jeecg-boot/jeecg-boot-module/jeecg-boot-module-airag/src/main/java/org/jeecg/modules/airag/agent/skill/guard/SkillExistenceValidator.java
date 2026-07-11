package org.jeecg.modules.airag.agent.skill.guard;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Skill 存在性校验器。
 */
@Component
public class SkillExistenceValidator {
    private final ObjectProvider<SkillRegistry> skillRegistryProvider;

    public SkillExistenceValidator(ObjectProvider<SkillRegistry> skillRegistryProvider) {
        this.skillRegistryProvider = skillRegistryProvider;
    }

    /**
     * 校验 skillCode 是否存在。
     *
     * @param skillCode 编码
     */
    public void validateSkillExists(String skillCode) {
        if (!StringUtils.hasText(skillCode)) {
            throw new JeecgBootException("skillCode不能为空");
        }
        SkillRegistry skillRegistry = this.skillRegistryProvider == null ? null : this.skillRegistryProvider.getIfAvailable();
        if (skillRegistry == null || skillRegistry.findSkill(skillCode).isEmpty()) {
            throw new JeecgBootException("未找到Skill: " + skillCode);
        }
    }
}
