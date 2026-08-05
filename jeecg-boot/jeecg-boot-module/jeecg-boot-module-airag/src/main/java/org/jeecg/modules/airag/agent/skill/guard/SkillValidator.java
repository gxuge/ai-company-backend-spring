package org.jeecg.modules.airag.agent.skill.guard;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Skill 定义校验器。
 */
@Component
public class SkillValidator {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9-_.]{0,63}$");

    /**
     * 校验 Skill 定义。
     *
     * @param definition 定义
     */
    public void validateDefinition(SkillDefinition definition) {
        if (definition == null) {
            throw new JeecgBootException("Skill定义不能为空");
        }
        validateText("code", definition.getCode(), CODE_PATTERN);
        validateText("name", definition.getName(), null);
        if (!StringUtils.hasText(definition.getDescription())) {
            throw new JeecgBootException("Skill description不能为空");
        }
        if (!StringUtils.hasText(definition.getDomain())) {
            throw new JeecgBootException("Skill domain不能为空");
        }
        if (!StringUtils.hasText(definition.getVersion())) {
            throw new JeecgBootException("Skill version不能为空");
        }
    }

    private void validateText(String field, String value, Pattern pattern) {
        if (!StringUtils.hasText(value)) {
            throw new JeecgBootException("Skill " + field + "不能为空");
        }
        if (pattern != null && !pattern.matcher(value).matches()) {
            throw new JeecgBootException("Skill " + field + "格式不合法: " + value);
        }
    }
}
