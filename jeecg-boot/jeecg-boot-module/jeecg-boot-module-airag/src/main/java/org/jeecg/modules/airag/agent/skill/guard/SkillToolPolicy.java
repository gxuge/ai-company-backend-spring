package org.jeecg.modules.airag.agent.skill.guard;

import java.util.List;

/**
 * Skill 工具策略扩展点。
 */
public interface SkillToolPolicy {
    /**
     * 判断当前 Skill 是否允许调用某个工具。
     *
     * @param skillCode Skill 编码
     * @param toolName 工具名
     * @return 是否允许
     */
    boolean isToolAllowed(String skillCode, String toolName);

    /**
     * 返回当前 Skill 允许的工具列表。
     *
     * @param skillCode Skill 编码
     * @return 工具名列表
     */
    List<String> allowedTools(String skillCode);
}
