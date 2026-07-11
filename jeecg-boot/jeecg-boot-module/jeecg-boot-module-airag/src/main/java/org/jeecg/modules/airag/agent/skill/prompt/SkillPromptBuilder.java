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
        sb.append("你现在可以按需读取 Skill。").append('\n');
        sb.append("可用 Skill 列表：").append('\n');
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
        sb.append("读取方式：readSkill(skillCode)").append('\n');
        sb.append("规则：如果某个 Skill 适合当前任务，先调用 readSkill 读取完整 SKILL.md；").append('\n');
        sb.append("不要猜测未读取的 Skill 正文；不要一次性读取无关 Skill；").append('\n');
        sb.append("如果当前任务不需要 Skill，可以继续按现有上下文执行。");
        return sb.toString();
    }
}
