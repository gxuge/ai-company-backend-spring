package org.jeecg.modules.airag.agent.interaction;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class AgentInteractionEventPublisherTest {

    @Test
    void shouldPublishConfirmStartAndEndForToolInteraction() {
        AgentEventPublisher eventPublisher = Mockito.mock(AgentEventPublisher.class);
        AgentInteractionEventPublisher publisher = new AgentInteractionEventPublisher(eventPublisher);
        AgentContext context = new AgentContext();
        List<Map<String, String>> options = List.of(
                Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                Map.of("label", "不满意，重新生成", "value", "REGENERATE")
        );
        Map<String, Object> interaction = Map.of(
                "interactionId", "confirm-1",
                "interactionType", "confirm",
                "toolName", "role_request_confirmation",
                "question", "你对这版角色满意吗？",
                "options", options
        );

        publisher.publishRequested(context, ToolCallResult.success("等待确认", interaction));
        publisher.publishResolved(context, interaction, "REGENERATE");

        Mockito.verify(eventPublisher).publishConfirmStart(
                context,
                "confirm.start",
                "role_request_confirmation",
                "你对这版角色满意吗？",
                options
        );
        Mockito.verify(eventPublisher).publishConfirmEnd(
                Mockito.eq(context),
                Mockito.eq("confirm.end"),
                Mockito.eq("role_request_confirmation"),
                Mockito.eq("你对这版角色满意吗？"),
                Mockito.eq(options),
                Mockito.argThat(data -> "confirm-1".equals(data.get("interactionId"))
                        && "REGENERATE".equals(data.get("optionValue"))
                        && "REGENERATE".equals(data.get("value")))
        );
    }
}
