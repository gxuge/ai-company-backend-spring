package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.modules.airag.agent.service.TsAgentChatMessageEventService;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.sse.SsePayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class AgentEventPublisherTest {

    private TsAgentChatMessageEventService eventService;
    private SseConnectionManager sseConnectionManager;
    private AgentEventPublisher eventPublisher;
    private AgentContext context;

    @BeforeEach
    void setUp() {
        this.eventService = Mockito.mock(TsAgentChatMessageEventService.class);
        this.sseConnectionManager = Mockito.mock(SseConnectionManager.class);
        RedisTemplate redisTemplate = Mockito.mock(RedisTemplate.class);
        this.eventPublisher = new AgentEventPublisher(this.eventService, this.sseConnectionManager, redisTemplate);

        this.context = new AgentContext();
        this.context.setMessageId("10001");
        this.context.setSessionId(1001L);
        this.context.setRunId("run-1");
        this.context.setTraceId("trace-1");
        this.context.setTurnId("turn-1");
        this.context.setSseConnectionKey("sse-1");
        this.context.setSenderType("sub_agent");
        this.context.setAgentCode("role_task_agent");
        this.context.setUserInput("创建一个女性角色");
        this.context.putAttribute("taskDescription", "完善角色设定");
        this.context.putAttribute("agentStepIndex", 2);
    }

    @Test
    void shouldPersistOneCompleteSubAgentEventOnEnd() {
        this.eventPublisher.publishSubAgentStart(this.context, "Role Task Agent", null);
        Mockito.verifyNoInteractions(this.eventService);

        AgentResult result = AgentResult.success("角色设定完成");
        result.setStructuredResult(Map.of("roleId", 88L));
        this.context.markCurrentNode("roleCreateDialogNode", "llm");
        this.context.markResultNode("roleCreateDialogNode", "llm", "角色设定完成", true);
        this.eventPublisher.publishSubAgentEnd(this.context, "Role Task Agent", result, null);

        ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> dataCaptor = mapCaptor();
        Mockito.verify(this.eventService).saveEvent(
                eventIdCaptor.capture(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("subagent"),
                Mockito.eq("role_task_agent"),
                Mockito.eq("roleCreateDialogNode"),
                Mockito.eq("llm"),
                Mockito.eq("角色设定完成"),
                Mockito.eq(1),
                dataCaptor.capture()
        );
        Assertions.assertEquals(eventIdCaptor.getValue(), this.context.getLastCompletedSubAgentEventId());

        Map<String, Object> data = dataCaptor.getValue();
        Map<String, Object> input = castMap(data.get("input"));
        Map<String, Object> output = castMap(data.get("output"));
        Map<String, Object> metrics = castMap(data.get("metrics"));
        Assertions.assertEquals("创建一个女性角色", input.get("userInput"));
        Assertions.assertEquals("完善角色设定", input.get("taskDescription"));
        Assertions.assertEquals("角色设定完成", output.get("content"));
        Assertions.assertEquals(Map.of("roleId", 88L), output.get("structuredResult"));
        Assertions.assertTrue(data.containsKey("error"));
        Assertions.assertNull(data.get("error"));
        Assertions.assertEquals(2, metrics.get("stepIndex"));
        Assertions.assertTrue(((Number) metrics.get("durationMs")).longValue() >= 0L);
    }

    @Test
    void shouldPersistCurrentNodeWhenSubAgentFails() {
        this.eventPublisher.publishSubAgentStart(this.context, "Role Task Agent", null);
        this.context.markResultNode("roleCreateDialogNode", "llm", "上一节点回复", true);
        this.context.markCurrentNode("roleConfirmationNode", "tool");

        this.eventPublisher.publishSubAgentEnd(
                this.context,
                "Role Task Agent",
                AgentResult.failed("流程确认失败"),
                null
        );

        Mockito.verify(this.eventService).saveEvent(
                Mockito.anyString(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("subagent"),
                Mockito.eq("role_task_agent"),
                Mockito.eq("roleConfirmationNode"),
                Mockito.eq("tool"),
                Mockito.eq("流程确认失败"),
                Mockito.eq(0),
                Mockito.anyMap()
        );
    }

    @Test
    void shouldPersistOneCompleteFailedToolEventOnEnd() {
        this.eventPublisher.publishSubAgentStart(this.context, "Role Task Agent", null);
        Map<String, Object> arguments = Map.of("name", "林雪", "gender", "女");
        this.eventPublisher.publishToolStart(
                this.context,
                "roleCreateNode",
                "create_role",
                Map.of("arguments", arguments)
        );
        this.eventPublisher.publishToolError(
                this.context,
                "roleCreateNode",
                "create_role",
                new IllegalStateException("角色创建失败"),
                null
        );

        Map<String, Object> endPayload = new LinkedHashMap<>();
        endPayload.put("toolArguments", arguments);
        this.eventPublisher.publishToolEnd(
                this.context,
                "roleCreateNode",
                "create_role",
                false,
                "角色创建失败",
                endPayload
        );

        ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> dataCaptor = mapCaptor();
        Mockito.verify(this.eventService).saveEvent(
                eventIdCaptor.capture(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("tool"),
                Mockito.eq("create_role"),
                Mockito.eq("roleCreateNode"),
                Mockito.eq("tool"),
                Mockito.eq("角色创建失败"),
                Mockito.eq(0),
                dataCaptor.capture()
        );

        Map<String, Object> data = dataCaptor.getValue();
        Map<String, Object> input = castMap(data.get("input"));
        Map<String, Object> error = castMap(data.get("error"));
        Map<String, Object> metrics = castMap(data.get("metrics"));
        Assertions.assertEquals(arguments, input.get("arguments"));
        Assertions.assertTrue(data.containsKey("output"));
        Assertions.assertNull(data.get("output"));
        Assertions.assertEquals("IllegalStateException", error.get("code"));
        Assertions.assertEquals("角色创建失败", error.get("message"));
        Assertions.assertEquals(eventIdCaptor.getValue(), metrics.get("toolCallId"));
        Assertions.assertNotNull(data.get("parentEventId"));
    }

    @Test
    void shouldNotPersistLlmEvents() {
        this.eventPublisher.publishLlmStart(this.context, "dialogNode", "role_dialog_v1");
        this.eventPublisher.publishLlmError(
                this.context,
                "dialogNode",
                "role_dialog_v1",
                new IllegalStateException("模型调用失败")
        );

        Mockito.verifyNoInteractions(this.eventService);
    }

    @Test
    void shouldNotPersistInternalTaskHandoffTool() {
        this.eventPublisher.publishToolStart(this.context, "mainNode", "task", null);
        this.eventPublisher.publishToolEnd(
                this.context,
                "mainNode",
                "task",
                true,
                "切换到角色子Agent",
                Map.of("targetSubAgent", "role_task_agent")
        );

        Mockito.verifyNoInteractions(this.eventService);
    }

    @Test
    void shouldSendConfirmationQuestionAndOptionsInToolEndSse() {
        this.eventPublisher.publishToolStart(this.context, "role_confirmation", "role_confirmation", null);
        Map<String, Object> toolData = new LinkedHashMap<>();
        toolData.put("question", "你对这版角色满意吗？");
        toolData.put("options", List.of(
                Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                Map.of("label", "不满意，重新生成", "value", "REGENERATE")
        ));
        this.eventPublisher.publishToolEnd(
                this.context,
                "role_confirmation",
                "role_confirmation",
                true,
                "需要用户确认",
                Map.of("toolData", toolData)
        );

        ArgumentCaptor<SsePayload> payloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("tool.end"),
                payloadCaptor.capture()
        );
        SsePayload payload = payloadCaptor.getValue();
        Assertions.assertEquals("你对这版角色满意吗？", payload.getQuestion());
        Assertions.assertEquals(
                List.of(
                        Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                        Map.of("label", "不满意，重新生成", "value", "REGENERATE")
                ),
                payload.getOptions()
        );
        Assertions.assertNull(payload.getData());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
