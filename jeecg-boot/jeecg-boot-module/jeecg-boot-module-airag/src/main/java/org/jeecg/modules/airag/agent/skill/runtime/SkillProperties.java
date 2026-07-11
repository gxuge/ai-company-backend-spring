package org.jeecg.modules.airag.agent.skill.runtime;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Skill 配置。
 */
@Data
@Component
public class SkillProperties {
    /**
     * Skill 根目录，相对工作目录。
     */
    @Value("${jeecg.airag.skill.root-dir:classpath*:deepagents/skills}")
    private String rootDir;
}
