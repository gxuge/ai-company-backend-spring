package org.jeecg.modules.airag.agent.node;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ConfirmationNodeTest {

    @Test
    void shouldPassOptionValueToConfirmationToolAndConsumeIt() {
        ToolRegistry toolRegistry = new ToolRegistry();
        ToolDefinition definition = new ToolDefinition();
        definition.setName("test_confirmation");
        definition.setExecutor((context, request) -> ToolCallResult.success(
                "confirmed",
                Map.of("optionValue", request.getArguments().get("optionValue"))
        ));
        toolRegistry.register(definition);
        ConfirmationNode node = new ConfirmationNode(
                "test_confirmation",
                "测试确认",
                "test_confirmation",
                toolRegistry
        ) {
        };
        AgentContext context = new AgentContext();
        context.putAttribute("optionValue", "ACCEPT_AND_CONTINUE");

        var result = node.execute(context);

        Assertions.assertTrue(node.hasOptionValue(context));
        Assertions.assertEquals("ACCEPT_AND_CONTINUE", node.resolveOptionValue(context));
        Assertions.assertEquals(
                "ACCEPT_AND_CONTINUE",
                ((Map<?, ?>) result.getData().get("toolData")).get("optionValue")
        );
        node.consumeOptionValue(context);
        Assertions.assertFalse(node.hasOptionValue(context));
    }

    @Test
    void shouldSupportCustomContextAndArgumentFieldNames() {
        ToolRegistry toolRegistry = new ToolRegistry();
        ToolDefinition definition = new ToolDefinition();
        definition.setName("custom_confirmation");
        definition.setExecutor((context, request) -> ToolCallResult.success(
                "confirmed",
                request.getArguments()
        ));
        toolRegistry.register(definition);
        ConfirmationNode node = new ConfirmationNode(
                "custom_confirmation",
                "自定义确认",
                "custom_confirmation",
                toolRegistry,
                "selectedValue",
                "selected_value"
        ) {
            @Override
            protected Map<String, Object> buildConfirmationArguments(AgentContext context) {
                return Map.of("source", "frontend");
            }
        };
        AgentContext context = new AgentContext();
        context.putAttribute("selectedValue", "REGENERATE");

        var result = node.execute(context);
        Map<?, ?> toolData = (Map<?, ?>) result.getData().get("toolData");

        Assertions.assertEquals("REGENERATE", toolData.get("selected_value"));
        Assertions.assertEquals("frontend", toolData.get("source"));
        Assertions.assertEquals("selectedValue", node.getOptionValueAttribute());
        Assertions.assertEquals("selected_value", node.getOptionValueArgument());
    }
}
