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

    @Test
    void shouldPersistGeneratedContentInsteadOfConfirmationQuestion() {
        TsAgentChatReplyServiceImpl service = new TsAgentChatReplyServiceImpl();
        AgentContext context = new AgentContext();
        context.setLatestContent("角色设定已经整理完成：林夏是一位年轻的男性魔法师。");
        AgentResult waiting = AgentResult.waitingUser("你对这版角色满意吗？");
        waiting.getData().put("interactionId", "interaction-1");
        waiting.getData().put("interactionType", "confirm");
        waiting.getData().put("question", "你对这版角色满意吗？");

        String content = ReflectionTestUtils.invokeMethod(
                service,
                "resolveAssistantContent",
                waiting,
                context
        );

        Assertions.assertEquals(
                "角色设定已经整理完成：林夏是一位年轻的男性魔法师。",
                content
        );
    }

    @Test
    void shouldKeepOrdinaryWaitingQuestionAsAssistantContent() {
        TsAgentChatReplyServiceImpl service = new TsAgentChatReplyServiceImpl();
        AgentContext context = new AgentContext();
        context.setLatestContent("上一轮内容");
        AgentResult waiting = AgentResult.waitingUser("请补充角色年龄。");

        String content = ReflectionTestUtils.invokeMethod(
                service,
                "resolveAssistantContent",
                waiting,
                context
        );

        Assertions.assertEquals("请补充角色年龄。", content);
    }

    @Test
    void shouldNotPersistConfirmationQuestionWhenGeneratedContentIsMissing() {
        TsAgentChatReplyServiceImpl service = new TsAgentChatReplyServiceImpl();
        AgentResult waiting = AgentResult.waitingUser("你对这版角色满意吗？");
        waiting.getData().put("interactionId", "interaction-2");
        waiting.getData().put("interactionType", "confirm");
        waiting.getData().put("question", "你对这版角色满意吗？");

        String content = ReflectionTestUtils.invokeMethod(
                service,
                "resolveAssistantContent",
                waiting,
                new AgentContext()
        );

        Assertions.assertNull(content);
    }

    @Test
    void shouldPersistInterruptedFlowStateAndMessageStatus() {
        TsAgentChatReplyServiceImpl service = new TsAgentChatReplyServiceImpl();
        TsAgentChatSession session = new TsAgentChatSession();
        AgentContext context = new AgentContext();
        context.setResumeNodeName("role_create_dialog");
        context.setActiveStage("collecting");
        context.putAttribute("roleCoreResultJson", "{\"name\":\"林夏\"}");
        AgentResult interrupted = AgentResult.interrupted("已停止");
        AgentFlowStateSupport.attachResumeData(interrupted, context);

        ReflectionTestUtils.invokeMethod(
                service,
                "updateFlowResumeState",
                session,
                context,
                AgentFlowStateSupport.ROLE_AGENT_CODE,
                interrupted
        );

        Assertions.assertEquals("role_create_dialog", session.getActiveNodeName());
        Assertions.assertEquals("collecting", session.getActiveStage());
        Assertions.assertTrue(session.getAgentFlowStateJson().contains("roleCoreResultJson"));
        Assertions.assertEquals(
                "interrupted",
                ReflectionTestUtils.invokeMethod(service, "toMessageStatus", AgentResult.Status.INTERRUPTED)
        );
    }

    @Test
    void shouldPreferInterruptedPartialContentForDisplay() {
        TsAgentChatReplyServiceImpl service = new TsAgentChatReplyServiceImpl();
        AgentContext context = new AgentContext();
        context.setLatestContent("不应覆盖中断文本");
        context.putAttribute("interruptedLlmContent", "已经输出的部分内容");

        String content = ReflectionTestUtils.invokeMethod(
                service,
                "resolveAssistantContent",
                AgentResult.interrupted("用户停止"),
                context
        );

        Assertions.assertEquals("已经输出的部分内容", content);
    }
}
