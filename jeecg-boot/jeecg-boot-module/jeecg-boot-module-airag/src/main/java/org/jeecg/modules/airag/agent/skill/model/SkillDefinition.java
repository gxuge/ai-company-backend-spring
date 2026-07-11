package org.jeecg.modules.airag.agent.skill.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill 定义。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillDefinition {
    /**
     * Skill 编码。
     */
    private String code;
    /**
     * Skill 名称。
     */
    private String name;
    /**
     * Skill 描述。
     */
    private String description;
    /**
     * 适用域。
     */
    private String domain;
    /**
     * 版本号。
     */
    private String version;
    /**
     * 允许使用的工具。
     */
    private List<String> allowedTools = new ArrayList<>();
    /**
     * 必填输入字段。
     */
    private List<String> requiredInputs = new ArrayList<>();
    /**
     * 可选输入字段。
     */
    private List<String> optionalInputs = new ArrayList<>();
    /**
     * 输出字段。
     */
    private List<String> outputs = new ArrayList<>();
    /**
     * 可追问字段。
     */
    private List<String> clarifyInputs = new ArrayList<>();
    /**
     * 元数据。
     */
    private Map<String, Object> metadata = new LinkedHashMap<>();
    /**
     * Markdown 正文。
     */
    private String content;
    /**
     * 资源列表。
     */
    private List<SkillResource> resources = new ArrayList<>();

    /**
     * 复制一份不包含正文的元信息。
     *
     * @return SkillDefinition
     */
    public SkillDefinition copyWithoutContent() {
        SkillDefinition copy = new SkillDefinition();
        copy.setCode(this.code);
        copy.setName(this.name);
        copy.setDescription(this.description);
        copy.setDomain(this.domain);
        copy.setVersion(this.version);
        copy.setAllowedTools(this.allowedTools == null ? new ArrayList<>() : new ArrayList<>(this.allowedTools));
        copy.setRequiredInputs(this.requiredInputs == null ? new ArrayList<>() : new ArrayList<>(this.requiredInputs));
        copy.setOptionalInputs(this.optionalInputs == null ? new ArrayList<>() : new ArrayList<>(this.optionalInputs));
        copy.setOutputs(this.outputs == null ? new ArrayList<>() : new ArrayList<>(this.outputs));
        copy.setClarifyInputs(this.clarifyInputs == null ? new ArrayList<>() : new ArrayList<>(this.clarifyInputs));
        copy.setMetadata(this.metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(this.metadata));
        copy.setResources(new ArrayList<>());
        copy.setContent(null);
        return copy;
    }
}
