package org.jeecg.modules.airag.agent.node;

import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class OptionsNodeTest {

    @Test
    void shouldReturnQuestionAndOptionsWhenOptionValueIsMissing() {
        OptionsNode node = buildNode();
        AgentContext context = new AgentContext();

        var result = node.execute(context);

        Assertions.assertEquals(NodeKind.OPTIONS, node.kind());
        Assertions.assertEquals("options.start", node.getStartSseName());
        Assertions.assertEquals("options.end", node.getEndSseName());
        Assertions.assertEquals(OptionsNode.ACTION_WAIT_OPTIONS, result.getAction());
        Assertions.assertEquals("请选择角色风格", result.getData().get("question"));
        Assertions.assertEquals(node.getOptions(), result.getData().get("options"));
    }

    @Test
    void shouldReturnSelectedOptionAndConsumeFrontendOptionValue() {
        OptionsNode node = buildNode();
        AgentContext context = new AgentContext();
        context.putAttribute("selectedValue", "MODERN");

        var result = node.execute(context);

        Assertions.assertTrue(node.hasOptionValue(context));
        Assertions.assertEquals(OptionsNode.ACTION_OPTION_SELECTED, result.getAction());
        Assertions.assertEquals("MODERN", result.getData().get("optionValue"));
        Assertions.assertEquals(
                Map.of("label", "现代都市", "optionValue", "MODERN"),
                result.getData().get("selectedOption")
        );
        Assertions.assertEquals(List.of(), result.getData().get("options"));
        node.consumeOptionValue(context);
        Assertions.assertFalse(node.hasOptionValue(context));
    }

    @Test
    void shouldWaitAgainWhenOptionValueIsInvalid() {
        OptionsNode node = buildNode();
        AgentContext context = new AgentContext();
        context.putAttribute("selectedValue", "UNKNOWN");

        var result = node.execute(context);

        Assertions.assertEquals(OptionsNode.ACTION_INVALID_OPTION, result.getAction());
        Assertions.assertEquals(node.getOptions(), result.getData().get("options"));
        Assertions.assertNull(result.getData().get("selectedOption"));
    }

    private OptionsNode buildNode() {
        return new OptionsNode(
                "test_options",
                "测试候选项",
                "options",
                "请选择角色风格",
                List.of(
                        Map.of("label", "现代都市", "optionValue", "MODERN"),
                        Map.of("label", "古风仙侠", "optionValue", "XIANXIA")
                ),
                "selectedValue"
        ) {
        };
    }
}
