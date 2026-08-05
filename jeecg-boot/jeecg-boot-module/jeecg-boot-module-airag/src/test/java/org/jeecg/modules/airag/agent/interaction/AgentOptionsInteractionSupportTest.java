package org.jeecg.modules.airag.agent.interaction;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.tool.options.AgentOptionsToolService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class AgentOptionsInteractionSupportTest {

    @Test
    void shouldResumeConversationWithSelectedLabel() {
        AgentContext context = new AgentContext();
        Map<String, Object> pending = createPending(context);
        context.putAttribute(UserInteractionSupport.ATTR_INTERACTION_ID, pending.get("interactionId"));
        context.putAttribute(UserInteractionSupport.ATTR_OPTION_VALUE, "candidate_2");

        boolean resumed = AgentOptionsInteractionSupport.resumeConversation(context, pending);

        Assertions.assertTrue(resumed);
        Assertions.assertEquals("开始生成吧 🎨", context.getUserInput());
        Assertions.assertFalse(UserInteractionSupport.hasPending(context));
    }

    @Test
    void shouldAllowFreeTextAndKeepWaitingWithoutAnswer() {
        AgentContext waitingContext = new AgentContext();
        Map<String, Object> waitingPending = createPending(waitingContext);

        Assertions.assertFalse(
                AgentOptionsInteractionSupport.resumeConversation(waitingContext, waitingPending)
        );
        AgentResult waitingResult = AgentOptionsInteractionSupport.waitingResult(
                waitingContext,
                waitingPending,
                "role_create_dialog",
                "dialog"
        );
        Assertions.assertEquals(AgentResult.Status.WAITING_USER, waitingResult.getStatus());
        Assertions.assertEquals("role_create_dialog", waitingResult.getData().get("resumeNodeName"));
        Assertions.assertEquals(waitingPending.get("options"), waitingResult.getData().get("options"));

        AgentContext freeTextContext = new AgentContext();
        Map<String, Object> freeTextPending = createPending(freeTextContext);
        freeTextContext.setUserInput("我想换一种风格");

        Assertions.assertTrue(
                AgentOptionsInteractionSupport.resumeConversation(freeTextContext, freeTextPending)
        );
        Assertions.assertEquals("我想换一种风格", freeTextContext.getUserInput());
        Assertions.assertFalse(UserInteractionSupport.hasPending(freeTextContext));
    }

    private Map<String, Object> createPending(AgentContext context) {
        return UserInteractionSupport.createPending(
                context,
                AgentOptionsToolService.INTERACTION_TYPE,
                AgentOptionsToolService.TOOL_NAME,
                "role_create_dialog",
                "role_create_dialog",
                "接下来想做什么呀？✨",
                null,
                List.of(
                        Map.of("label", "继续完善设定～", "optionValue", "candidate_1"),
                        Map.of("label", "开始生成吧 🎨", "optionValue", "candidate_2")
                )
        );
    }
}
