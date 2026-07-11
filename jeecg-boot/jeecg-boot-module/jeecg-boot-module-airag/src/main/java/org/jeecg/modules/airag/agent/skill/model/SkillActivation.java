package org.jeecg.modules.airag.agent.skill.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill 激活状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillActivation {
    /**
     * 当前激活的 Skill 编码。
     */
    private String activeSkillCode;
    /**
     * 已加载的 Skill 编码列表。
     */
    private List<String> loadedSkillCodes = new ArrayList<>();
    /**
     * 允许的工具列表。
     */
    private List<String> allowedTools = new ArrayList<>();
}
