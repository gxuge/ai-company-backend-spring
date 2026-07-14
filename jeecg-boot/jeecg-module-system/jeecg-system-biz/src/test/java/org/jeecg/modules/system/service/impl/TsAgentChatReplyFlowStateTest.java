package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentRegistry;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.system.entity.TsAgentChatSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TsAgentChatReplyFlowStateTest {

    @Test
    void shouldPersistWaitingSubAgentStateAndClearItForMainAgent() {
        TsAgentChatReplyServiceImpl service = new TsAgentChatReplyServiceImpl();
        TsAgentChatSession session = new TsAgentChatSession();
        AgentContext context = new AgentContext();
        context.setResumeNodeName("role_create_dialog");
        context.setActiveStage("confirmation");
        context.putAttribute("roleCoreResultJson", "{\"name\":\"林夏\"}");
        AgentResult waiting = AgentResult.waitingUser("是否继续生成形象？");
        AgentFlowStateSupport.attachResumeData(waiting, context);

        ReflectionTestUtils.invokeMethod(
                service,
                "updateFlowResumeState",
                session,
                context,
                AgentFlowStateSupport.ROLE_AGENT_CODE,
                waiting
        );

        Assertions.assertEquals("role_create_dialog", session.getActiveNodeName());
        Assertions.assertEquals("confirmation", session.getActiveStage());
        Assertions.assertTrue(session.getAgentFlowStateJson().contains("roleCoreResultJson"));

        ReflectionTestUtils.invokeMethod(
                service,
                "updateFlowResumeState",
                session,
                context,
                AgentRegistry.MAIN_AGENT_CODE,
                AgentResult.success("已完成")
        );

        Assertions.assertNull(session.getActiveNodeName());
        Assertions.assertNull(session.getActiveStage());
        Assertions.assertNull(session.getAgentFlowStateJson());
    }
}
