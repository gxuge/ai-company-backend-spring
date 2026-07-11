package org.jeecg.modules.airag.agent.skill.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill 资源。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillResource {
    /**
     * 资源类型。
     */
    private String type;
    /**
     * 相对路径。
     */
    private String path;
    /**
     * 资源名称。
     */
    private String name;
    /**
     * 资源内容。
     */
    private String content;
}
