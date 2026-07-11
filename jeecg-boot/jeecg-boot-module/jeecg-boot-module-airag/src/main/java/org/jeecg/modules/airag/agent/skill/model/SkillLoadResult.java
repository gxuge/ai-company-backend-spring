package org.jeecg.modules.airag.agent.skill.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill 运行时准备结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillLoadResult {
    /**
     * 任务域。
     */
    private String domain;
    /**
     * TopK。
     */
    private Integer topK;
    /**
     * 候选 Skill。
     */
    private List<SkillDefinition> candidateSkills = new ArrayList<>();
    /**
     * Skill 索引提示词。
     */
    private String skillIndexPrompt;
    /**
     * 激活状态。
     */
    private SkillActivation activation;
    /**
     * 扩展元数据。
     */
    private Map<String, Object> metadata = new LinkedHashMap<>();

    /**
     * 转为简单 Map。
     *
     * @return Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("domain", this.domain);
        map.put("topK", this.topK);
        map.put("skillIndexPrompt", this.skillIndexPrompt);
        map.put("candidateSkills", this.candidateSkills);
        map.put("activation", this.activation);
        map.put("metadata", this.metadata);
        return map;
    }
}
