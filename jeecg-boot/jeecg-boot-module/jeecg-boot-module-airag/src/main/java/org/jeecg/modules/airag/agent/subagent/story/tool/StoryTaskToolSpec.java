package org.jeecg.modules.airag.agent.subagent.story.tool;

import java.util.List;

/**
 * 故事子 Agent 预留工具清单。
 *
 * <p>这里只保存未来会接入的工具名，暂不实现任何业务执行逻辑。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
public final class StoryTaskToolSpec {

    /**
     * 预留工具名。
     */
    public static final String STORY_FULL_GENERATE_PRESET = "story_full_generate_preset";
    /**
     * 预留工具名。
     */
    public static final String STORY_FULL_GENERATE = "story_full_generate";
    /**
     * 故事确认交互工具。
     */
    public static final String STORY_REQUEST_CONFIRMATION = "story_request_confirmation";
    /**
     * 用户确认满意后显式继续生成故事背景与场景。
     */
    public static final String STORY_CONTINUE_GENERATION = "story_continue_generation";
    /**
     * 故事背景 / 场景生成工具。
     */
    public static final String STORY_GENERATE_SCENE = "story_generate_scene";

    /**
     * 当前故事链路的预留工具列表。
     */
    public static final List<String> TOOL_NAMES = List.of(
            STORY_FULL_GENERATE_PRESET,
            STORY_FULL_GENERATE,
            STORY_REQUEST_CONFIRMATION,
            STORY_CONTINUE_GENERATION,
            STORY_GENERATE_SCENE
    );

    private StoryTaskToolSpec() {
    }
}
