package org.jeecg.modules.airag.agent.tool.options;

import org.jeecg.modules.airag.agent.error.AgentErrorException;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class AgentOptionsToolServiceTest {

    @Test
    void shouldCreateCandidateOptionsWithBackendGeneratedValues() {
        ToolRegistry toolRegistry = new ToolRegistry();
        AgentOptionsToolService service = new AgentOptionsToolService(toolRegistry);
        service.registerTool();
        AgentContext context = new AgentContext();
        context.setCurrentNodeName("role_create_dialog");
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(AgentOptionsToolService.TOOL_NAME);
        request.setArguments(Map.of(
                "question", "接下来想做什么呀？✨",
                "options", List.of("继续完善设定～", "开始生成吧 🎨")
        ));

        ToolCallResult result = toolRegistry.execute(context, request);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("options", result.getContentType());
        Map<String, Object> pending = UserInteractionSupport.getPending(context);
        Assertions.assertEquals("role_create_dialog", pending.get("sourceNode"));
        Assertions.assertEquals("role_create_dialog", pending.get("resumeNode"));
        Assertions.assertEquals(AgentOptionsToolService.TOOL_NAME, pending.get("toolName"));
        Assertions.assertEquals(
                List.of(
                        Map.of("label", "继续完善设定～", "optionValue", "candidate_1"),
                        Map.of("label", "开始生成吧 🎨", "optionValue", "candidate_2")
                ),
                pending.get("options")
        );
    }

    @Test
    void shouldRejectDuplicateOrOversizedOptionSets() {
        ToolRegistry toolRegistry = new ToolRegistry();
        AgentOptionsToolService service = new AgentOptionsToolService(toolRegistry);
        service.registerTool();
        AgentContext context = new AgentContext();
        ToolCallRequest duplicateRequest = new ToolCallRequest();
        duplicateRequest.setToolName(AgentOptionsToolService.TOOL_NAME);
        duplicateRequest.setArguments(Map.of(
                "question", "选一个吧",
                "options", List.of("继续", "继续")
        ));
        ToolCallRequest oversizedRequest = new ToolCallRequest();
        oversizedRequest.setToolName(AgentOptionsToolService.TOOL_NAME);
        oversizedRequest.setArguments(Map.of(
                "question", "选一个吧",
                "options", List.of("一", "二", "三", "四", "五")
        ));

        Assertions.assertThrows(
                AgentErrorException.class,
                () -> toolRegistry.execute(context, duplicateRequest)
        );
        Assertions.assertThrows(
                AgentErrorException.class,
                () -> toolRegistry.execute(context, oversizedRequest)
        );
    }
}
