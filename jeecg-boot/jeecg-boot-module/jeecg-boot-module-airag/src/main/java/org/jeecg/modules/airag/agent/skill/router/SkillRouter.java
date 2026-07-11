package org.jeecg.modules.airag.agent.skill.router;

import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;

import java.util.List;

/**
 * Skill 路由器。
 */
public interface SkillRouter {
    /**
     * 路由候选 Skill。
     *
     * @param userInput 用户输入
     * @param domain 域
     * @param topK 返回数量
     * @return 候选 Skill 列表
     */
    List<SkillDefinition> route(String userInput, String domain, int topK);
}
