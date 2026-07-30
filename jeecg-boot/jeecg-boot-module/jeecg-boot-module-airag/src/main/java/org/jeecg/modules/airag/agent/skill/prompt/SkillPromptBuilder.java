package org.jeecg.modules.airag.agent.skill.prompt;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Skill 索引提示词构建器。
 */
@Component
public class SkillPromptBuilder {
    /**
     * 生成 Skill 索引提示词。
     *
     * @param skills Skill 列表
     * @return 提示词
     */
    public String buildSkillIndexPrompt(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("You may load skills when they are relevant to the current task.").append('\n');
        sb.append("Available skills:").append('\n');
        for (int i = 0; i < skills.size(); i++) {
            SkillDefinition skill = skills.get(i);
            if (skill == null) {
                continue;
            }
            sb.append(i + 1).append(". ");
            sb.append("code=").append(oConvertUtils.getString(skill.getCode()));
            sb.append(" | name=").append(oConvertUtils.getString(skill.getName()));
            sb.append(" | description=").append(oConvertUtils.getString(skill.getDescription()));
            sb.append('\n');
        }
        sb.append('\n');
        sb.append("Load with: readSkill(skillCode)").append('\n');
        sb.append("Rules: if a skill matches the current task, call readSkill before relying on its full instructions.").append('\n');
        sb.append("Do not guess the contents of an unloaded skill or load unrelated skills in bulk.").append('\n');
        sb.append("If no skill is needed, continue using the current context.");
        return sb.toString();
    }
}
