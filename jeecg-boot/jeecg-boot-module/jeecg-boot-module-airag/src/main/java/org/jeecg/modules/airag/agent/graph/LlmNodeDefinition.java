package org.jeecg.modules.airag.agent.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 节点定义。
 *
 * 负责承载原来分散在子 Agent 里的节点级配置，例如 skill、prompt、工具范围、
 * 输入输出约束和下一步触发条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class LlmNodeDefinition extends DeepAgentDefinition {
    /**
     * 提示词编码。
     */
    private String promptCode;
    /**
     * 提示词版本。
     */
    private String promptVersion;
    /**
     * 原始系统提示词模板。
     */
    private String systemPromptTemplate;
    /**
     * 原始用户提示词模板。
     */
    private String userPromptTemplate;
    /**
     * 是否将业务会话历史按原生 ChatMessage 结构传给模型。
     */
    private boolean conversationHistoryEnabled;
    /**
     * 输入约束说明。
     */
    private String inputConstraints;
    /**
     * 输出约束说明。
     */
    private String outputConstraints;
    /**
     * 下一步触发条件说明。
     */
    private String nextStepCondition;

    /**
     * 转为 Map，便于塞入上下文或调试。
     *
     * @return Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>(super.toMap());
        map.put("promptCode", this.promptCode);
        map.put("promptVersion", this.promptVersion);
        map.put("systemPromptTemplate", this.systemPromptTemplate);
        map.put("userPromptTemplate", this.userPromptTemplate);
        map.put("conversationHistoryEnabled", this.conversationHistoryEnabled);
        map.put("inputConstraints", this.inputConstraints);
        map.put("outputConstraints", this.outputConstraints);
        map.put("nextStepCondition", this.nextStepCondition);
        return map;
    }
}
