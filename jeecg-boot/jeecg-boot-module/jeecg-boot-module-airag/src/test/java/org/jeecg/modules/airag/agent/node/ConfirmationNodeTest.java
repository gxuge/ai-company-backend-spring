package org.jeecg.modules.airag.agent.node;

import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ConfirmationNodeTest {

    @Test
    void shouldReturnQuestionAndOptionsWhenOptionValueIsMissing() {
        ConfirmationNode node = buildNode();
        AgentContext context = new AgentContext();

        var result = node.execute(context);

        Assertions.assertEquals(NodeKind.CONFIRM, node.kind());
        Assertions.assertEquals("confirm.start", node.getStartSseName());
        Assertions.assertEquals("confirm.end", node.getEndSseName());
        Assertions.assertEquals("WAIT_CONFIRM", result.getAction());
        Assertions.assertEquals("是否继续？", result.getData().get("question"));
        Assertions.assertEquals(node.getOptions(), result.getData().get("options"));
    }

    @Test
    void shouldResolveAndConsumeFrontendOptionValue() {
        ConfirmationNode node = buildNode();
        AgentContext context = new AgentContext();
        context.putAttribute("selectedValue", "ACCEPT_AND_CONTINUE");

        var result = node.execute(context);

        Assertions.assertTrue(node.hasOptionValue(context));
        Assertions.assertEquals("ACCEPT_AND_CONTINUE", result.getAction());
        Assertions.assertEquals(
                Map.of("label", "继续", "optionValue", "ACCEPT_AND_CONTINUE"),
                result.getData().get("selectedOption")
        );
        Assertions.assertEquals(List.of(), result.getData().get("options"));
        node.consumeOptionValue(context);
        Assertions.assertFalse(node.hasOptionValue(context));
    }

    private ConfirmationNode buildNode() {
        return new ConfirmationNode(
                "test_confirmation",
                "测试确认",
                "confirm",
                "是否继续？",
                List.of(
                        Map.of("label", "继续", "value", "ACCEPT_AND_CONTINUE"),
                        Map.of("label", "返回", "value", "REGENERATE")
                ),
                "selectedValue"
        ) {
            @Override
            protected String resolveAction(String optionValue) {
                return optionValue;
            }
        };
    }
}
