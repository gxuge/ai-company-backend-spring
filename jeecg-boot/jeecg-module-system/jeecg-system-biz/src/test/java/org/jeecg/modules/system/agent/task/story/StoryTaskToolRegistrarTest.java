package org.jeecg.modules.system.agent.task.story;

import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryTaskToolRegistrarTest {

    @Test
    void storyFlowGateReturnsFrontendConfirmationOptions() {
        ToolRegistry toolRegistry = new ToolRegistry();
        StoryTaskToolRegistrar registrar = new StoryTaskToolRegistrar(toolRegistry, null);
        registrar.registerTools();

        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(StoryTaskToolSpec.STORY_FLOW_GATE);
        request.setArguments(Map.of("stage", "story_confirm"));

        ToolCallResult result = toolRegistry.execute(null, request);

        assertTrue(result.isSuccess());
        assertTrue(result.getData() instanceof Map<?, ?>);
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("WAIT_CONFIRM", data.get("action"));
        assertEquals("你对这版故事满意吗？", data.get("question"));
        assertEquals(
                List.of("满意，继续生成", "不满意，重新生成"),
                data.get("options")
        );
    }
}
