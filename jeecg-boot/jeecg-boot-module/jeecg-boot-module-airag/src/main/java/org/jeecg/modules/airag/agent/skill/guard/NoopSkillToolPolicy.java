package org.jeecg.modules.airag.agent.skill.guard;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Skill 工具策略空实现。
 */
@Component
public class NoopSkillToolPolicy implements SkillToolPolicy {
    @Override
    public boolean isToolAllowed(String skillCode, String toolName) {
        return true;
    }

    @Override
    public List<String> allowedTools(String skillCode) {
        return Collections.emptyList();
    }
}
