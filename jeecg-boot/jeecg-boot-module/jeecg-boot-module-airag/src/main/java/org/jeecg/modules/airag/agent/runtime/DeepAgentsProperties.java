package org.jeecg.modules.airag.agent.runtime;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DeepAgents 主链路配置。
 *
 * @author codex
 * @date 2026/7/9
 */
@Data
@Component
public class DeepAgentsProperties {
    /**
     * 是否默认开启 deepagents 主链路提示词与 skill 注入。
     */
    @Value("${jeecg.airag.deepagents.enabled:true}")
    private boolean enabled;

    /**
     * 默认 skill 域。为空时表示加载全部已注册 skill。
     */
    @Value("${jeecg.airag.deepagents.default-skill-domain:}")
    private String defaultSkillDomain;

    /**
     * 默认 skill TopK。
     */
    @Value("${jeecg.airag.deepagents.default-skill-top-k:3}")
    private Integer defaultSkillTopK;
}
