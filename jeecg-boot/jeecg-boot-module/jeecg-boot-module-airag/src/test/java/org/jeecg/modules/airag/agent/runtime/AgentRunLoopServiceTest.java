package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.modules.airag.agent.graph.Agent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

class AgentRunLoopServiceTest {

    private AgentRegistry agentRegistry;
    private AgentContextPreparer contextPreparer;
    private AgentRuntimeService agentRuntimeService;
    private AgentRunLoopService runLoopService;
    private Agent mainAgent;
    private Agent roleAgent;

    @BeforeEach
    void setUp() {
        this.agentRegistry = Mockito.mock(AgentRegistry.class);
        this.contextPreparer = Mockito.mock(AgentContextPreparer.class);
        this.agentRuntimeService = Mockito.mock(AgentRuntimeService.class);
        this.runLoopService = new AgentRunLoopService(
                this.agentRegistry,
                this.contextPreparer,
                this.agentRuntimeService
        );
        this.mainAgent = Mockito.mock(Agent.class);
        this.roleAgent = Mockito.mock(Agent.class);

        Mockito.when(this.agentRegistry.normalizeCode(Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class).trim());
        Mockito.when(this.agentRegistry.find(AgentRegistry.MAIN_AGENT_CODE))
                .thenReturn(Optional.of(this.mainAgent));
        Mockito.when(this.agentRegistry.find("role_task_agent"))
                .thenReturn(Optional.of(this.roleAgent));
        Mockito.when(this.agentRegistry.exists(AgentRegistry.MAIN_AGENT_CODE)).thenReturn(true);
        Mockito.when(this.agentRegistry.exists("role_task_agent")).thenReturn(true);
        Mockito.when(this.agentRegistry.isMainAgentCode(AgentRegistry.MAIN_AGENT_CODE)).thenReturn(true);
        Mockito.when(this.agentRegistry.isMainAgentCode("role_task_agent")).thenReturn(false);
    }

    @Test
    void shouldStayOnSubAgentAfterMainHandoffAndWaitingUser() {
        AgentContext context = new AgentContext();
        AgentResult handoff = AgentResult.handoffTo("role_task_agent", "继续补充角色设定");
        AgentResult waiting = AgentResult.waitingUser("请补充角色职业");
        Mockito.when(this.agentRegistry.normalizeStartingAgentCode(AgentRegistry.MAIN_AGENT_CODE))
                .thenReturn(AgentRegistry.MAIN_AGENT_CODE);
        Mockito.when(this.agentRuntimeService.execute(this.mainAgent, context)).thenReturn(handoff);
        Mockito.when(this.agentRuntimeService.execute(this.roleAgent, context)).thenReturn(waiting);

        AgentRunOutcome outcome = this.runLoopService.run(AgentRegistry.MAIN_AGENT_CODE, context);

        Assertions.assertSame(waiting, outcome.getResult());
        Assertions.assertEquals("role_task_agent", outcome.getLastAgentCode());
        Assertions.assertEquals(2, outcome.getSteps().size());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, outcome.getSteps().get(0).getAgentCode());
        Assertions.assertEquals("role_task_agent", outcome.getSteps().get(1).getAgentCode());
        Mockito.verify(this.contextPreparer).applyHandoff(context, handoff);
    }

    @Test
    void shouldStartNextRunDirectlyFromPersistedSubAgent() {
        AgentContext context = new AgentContext();
        AgentResult success = AgentResult.success("角色创建完成");
        Mockito.when(this.agentRegistry.normalizeStartingAgentCode("role_task_agent"))
                .thenReturn("role_task_agent");
        Mockito.when(this.agentRuntimeService.execute(this.roleAgent, context)).thenReturn(success);

        AgentRunOutcome outcome = this.runLoopService.run("role_task_agent", context);

        Assertions.assertSame(success, outcome.getResult());
        Assertions.assertEquals("role_task_agent", outcome.getLastAgentCode());
        Assertions.assertEquals(1, outcome.getSteps().size());
        Mockito.verify(this.agentRuntimeService, Mockito.never()).execute(this.mainAgent, context);
    }

    @Test
    void shouldReturnControlToMainAgentInSameRun() {
        AgentContext context = new AgentContext();
        AgentResult handoff = AgentResult.handoffTo(AgentRegistry.MAIN_AGENT_CODE, "改为创建故事");
        AgentResult success = AgentResult.success("已由主Agent重新处理");
        Mockito.when(this.agentRegistry.normalizeStartingAgentCode("role_task_agent"))
                .thenReturn("role_task_agent");
        Mockito.when(this.agentRuntimeService.execute(this.roleAgent, context)).thenReturn(handoff);
        Mockito.when(this.agentRuntimeService.execute(this.mainAgent, context)).thenReturn(success);

        AgentRunOutcome outcome = this.runLoopService.run("role_task_agent", context);

        Assertions.assertSame(success, outcome.getResult());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, outcome.getLastAgentCode());
        Assertions.assertEquals(2, outcome.getSteps().size());
        Mockito.verify(this.contextPreparer).applyHandoff(context, handoff);
    }

    @Test
    void shouldFailWhenHandoffTargetDoesNotExist() {
        AgentContext context = new AgentContext();
        AgentResult handoff = AgentResult.handoffTo("missing_agent", "未知任务");
        Mockito.when(this.agentRegistry.normalizeStartingAgentCode(AgentRegistry.MAIN_AGENT_CODE))
                .thenReturn(AgentRegistry.MAIN_AGENT_CODE);
        Mockito.when(this.agentRegistry.exists("missing_agent")).thenReturn(false);
        Mockito.when(this.agentRuntimeService.execute(this.mainAgent, context)).thenReturn(handoff);

        AgentRunOutcome outcome = this.runLoopService.run(AgentRegistry.MAIN_AGENT_CODE, context);

        Assertions.assertEquals(AgentResult.Status.FAILED, outcome.getResult().getStatus());
        Assertions.assertEquals(AgentRegistry.MAIN_AGENT_CODE, outcome.getLastAgentCode());
        Assertions.assertEquals("missing_agent", outcome.getResult().getData().get("targetAgentCode"));
        Assertions.assertEquals(1, outcome.getSteps().size());
        Mockito.verify(this.contextPreparer, Mockito.never()).applyHandoff(context, handoff);
    }

    @Test
    void shouldKeepLastActuallyExecutedAgentWhenHandoffLimitIsReached() {
        AgentContext context = new AgentContext();
        Mockito.when(this.agentRegistry.normalizeStartingAgentCode(AgentRegistry.MAIN_AGENT_CODE))
                .thenReturn(AgentRegistry.MAIN_AGENT_CODE);
        Mockito.when(this.agentRuntimeService.execute(this.mainAgent, context))
                .thenAnswer(invocation -> AgentResult.handoffTo("role_task_agent", "转角色"));
        Mockito.when(this.agentRuntimeService.execute(this.roleAgent, context))
                .thenAnswer(invocation -> AgentResult.handoffTo(AgentRegistry.MAIN_AGENT_CODE, "转主Agent"));

        AgentRunOutcome outcome = this.runLoopService.run(AgentRegistry.MAIN_AGENT_CODE, context);

        Assertions.assertEquals(AgentResult.Status.FAILED, outcome.getResult().getStatus());
        Assertions.assertEquals(AgentRunLoopService.MAX_AGENT_STEPS, outcome.getSteps().size());
        Assertions.assertEquals("role_task_agent", outcome.getLastAgentCode());
        Assertions.assertEquals("role_task_agent", outcome.getResult().getData().get("lastAgentCode"));
    }
}
