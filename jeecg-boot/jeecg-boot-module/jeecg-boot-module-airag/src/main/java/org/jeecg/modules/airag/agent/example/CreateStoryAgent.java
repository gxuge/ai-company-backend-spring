package org.jeecg.modules.airag.agent.example;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.Agent;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.springframework.stereotype.Component;

/**
 * 创建故事 Agent 示例。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class CreateStoryAgent implements Agent {
    /**
     * 节点执行器。
     */
    private final NodeRunner nodeRunner;
    /**
     * 参数澄清节点。
     */
    private final StoryClarifyArgsNode storyClarifyArgsNode;
    /**
     * 生成故事节点。
     */
    private final GenerateStoryToolNode generateStoryToolNode;
    /**
     * 默认生成故事节点。
     */
    private final GenerateStoryWithDefaultsToolNode generateStoryWithDefaultsToolNode;
    /**
     * 保存草稿节点。
     */
    private final SaveDraftToolNode saveDraftToolNode;
    /**
     * 总结节点。
     */
    private final FinalSummaryNode finalSummaryNode;

    /**
     * 构造函数。
     *
     * @param nodeRunner 节点执行器
     * @param storyClarifyArgsNode 参数澄清节点
     * @param generateStoryToolNode 生成故事节点
     * @param generateStoryWithDefaultsToolNode 默认生成故事节点
     * @param saveDraftToolNode 保存草稿节点
     * @param finalSummaryNode 总结节点
     */
    public CreateStoryAgent(NodeRunner nodeRunner,
                            StoryClarifyArgsNode storyClarifyArgsNode,
                            GenerateStoryToolNode generateStoryToolNode,
                            GenerateStoryWithDefaultsToolNode generateStoryWithDefaultsToolNode,
                            SaveDraftToolNode saveDraftToolNode,
                            FinalSummaryNode finalSummaryNode) {
        this.nodeRunner = nodeRunner;
        this.storyClarifyArgsNode = storyClarifyArgsNode;
        this.generateStoryToolNode = generateStoryToolNode;
        this.generateStoryWithDefaultsToolNode = generateStoryWithDefaultsToolNode;
        this.saveDraftToolNode = saveDraftToolNode;
        this.finalSummaryNode = finalSummaryNode;
    }

    @Override
    public String agentName() {
        return "create_story_agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        NodeResult clarifyResult = this.nodeRunner.run(context, this.storyClarifyArgsNode);
        String action = oConvertUtils.getString(clarifyResult.getAction(), "ASK_USER");
        if ("ASK_USER".equalsIgnoreCase(action)) {
            AgentResult result = AgentResult.waitingUser(clarifyResult.getContent());
            result.getData().putAll(clarifyResult.getData());
            return result;
        }
        if ("CALL_DEFAULT_TOOL".equalsIgnoreCase(action)) {
            this.nodeRunner.run(context, this.generateStoryWithDefaultsToolNode);
        } else {
            this.nodeRunner.run(context, this.generateStoryToolNode);
        }
        this.nodeRunner.run(context, this.saveDraftToolNode);
        NodeResult finalSummary = this.nodeRunner.run(context, this.finalSummaryNode);
        AgentResult result = AgentResult.success(finalSummary.getContent());
        result.getData().putAll(finalSummary.getData());
        return result;
    }
}
