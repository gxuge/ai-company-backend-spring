package org.jeecg.modules.airag.agent.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deep Agent 定义。
 *
 * 用于描述一个子 Agent 的技能、工具、权限和输出约束。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepAgentDefinition {
    /**
     * 子 Agent 名称。
     */
    private String name;
    /**
     * 子 Agent 描述。
     */
    private String description;
    /**
     * skill 所在 domain。
     */
    private String skillDomain;
    /**
     * skill 检索条数。
     */
    private Integer skillTopK = 3;
    /**
     * 子 Agent 关联的 skill code。
     */
    private List<String> skills = new ArrayList<>();
    /**
     * 子 Agent 允许使用的工具。
     */
    private List<String> tools = new ArrayList<>();
    /**
     * 子 Agent 权限约束。
     */
    private List<String> permissions = new ArrayList<>();
    /**
     * 输出格式约束。
     */
    private String responseFormat;
    /**
     * 扩展元数据。
     */
    private Map<String, Object> metadata = new LinkedHashMap<>();

    /**
     * 转为简单 Map，便于塞入上下文。
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", this.name);
        map.put("description", this.description);
        map.put("skillDomain", this.skillDomain);
        map.put("skillTopK", this.skillTopK);
        map.put("skills", this.skills);
        map.put("tools", this.tools);
        map.put("permissions", this.permissions);
        map.put("responseFormat", this.responseFormat);
        map.put("metadata", this.metadata);
        return map;
    }
}
