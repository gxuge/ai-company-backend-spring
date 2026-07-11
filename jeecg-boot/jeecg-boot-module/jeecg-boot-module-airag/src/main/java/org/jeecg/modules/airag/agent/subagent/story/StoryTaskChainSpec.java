package org.jeecg.modules.airag.agent.subagent.story;

import java.util.List;

/**
 * 故事子 Agent 的链路规格。
 *
 * <p>用于统一声明故事子流程的技能、工具和节点顺序。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
public final class StoryTaskChainSpec {

    /**
     * 子 Agent 名称。
     */
    public static final String SUB_AGENT_NAME = "story_task_agent";

    /**
     * 故事链路会用到的 skill。
     */
    public static final List<String> SKILLS = List.of(
            "story_create_dialog",
            "story_create_background"
    );

    /**
     * 故事链路会用到的 tool。
     */
    public static final List<String> TOOLS = List.of(
            "story_full_generate_preset",
            "story_full_generate",
            "story_flow_gate",
            "story_generate_scene"
    );

    /**
     * 节点执行顺序。
     */
    public static final List<String> CHAIN = List.of(
            "story_create_dialog",
            "story_flow_gate",
            "story_create_background"
    );

    private StoryTaskChainSpec() {
    }
}
