package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.modules.airag.agent.service.TsAgentChatMessageEventService;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.Date;
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
    void shouldOnlySendSubAgentEventsWithoutPersistingThem() {
        this.eventPublisher.publishSubAgentStart(this.context, "Role Task Agent", null);
        AgentResult result = AgentResult.success("角色设定完成");
        this.context.markCurrentNode("roleCreateDialogNode", "llm");
        this.context.markResultNode("roleCreateDialogNode", "llm", "角色设定完成", true);
        this.eventPublisher.publishSubAgentEnd(this.context, "Role Task Agent", result, null);

        Mockito.verifyNoInteractions(this.eventService);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("subagent.start"),
                Mockito.any()
        );
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("subagent.end"),
                Mockito.any()
        );
        Assertions.assertTrue(this.context.snapshotEvents().isEmpty());
        Assertions.assertNull(this.context.getLastCompletedSubAgentEventId());
    }

    @Test
    void shouldPersistOneCompleteFailedToolEventOnEnd() {
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
        Assertions.assertFalse(data.containsKey("parentEventId"));
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
    void shouldPersistEmbeddedToolAndSendSse() {
        Map<String, Object> arguments = Map.of("name", "林雪", "gender", "女");
        this.eventPublisher.publishToolStart(
                this.context,
                "role_create_dialog",
                "role_generate_role",
                Map.of("toolArguments", arguments)
        );
        this.eventPublisher.publishToolEnd(
                this.context,
                "role_create_dialog",
                "role_generate_role",
                true,
                "角色生成完成",
                Map.of("toolArguments", arguments, "toolData", Map.of("roleId", 88L))
        );

        Mockito.verify(this.eventService).saveEvent(
                Mockito.anyString(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("tool"),
                Mockito.eq("role_generate_role"),
                Mockito.eq("role_create_dialog"),
                Mockito.eq("tool"),
                Mockito.eq("角色生成完成"),
                Mockito.eq(1),
                Mockito.anyMap()
        );
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("tool.start"),
                Mockito.any()
        );
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("tool.end"),
                Mockito.any()
        );
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
    void shouldSendOnlyQuestionAndOptionsInConfirmStartSse() {
        List<Map<String, String>> options = List.of(
                Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                Map.of("label", "不满意，重新生成", "value", "REGENERATE")
        );
        this.eventPublisher.publishConfirmStart(
                this.context,
                "confirm.start",
                "role_confirmation",
                "你对这版角色满意吗？",
                options
        );

        ArgumentCaptor<Map<String, Object>> payloadCaptor = mapCaptor();
        Mockito.verify(this.sseConnectionManager).sendRaw(
                Mockito.eq("sse-1"),
                Mockito.eq("confirm.start"),
                payloadCaptor.capture()
        );
        Map<String, Object> payload = payloadCaptor.getValue();
        Assertions.assertEquals(Map.of(
                "question", "你对这版角色满意吗？",
                "options", options
        ), payload);
    }

    @Test
    void shouldPersistConfirmSelectedValue() {
        TsAgentChatMessageEventEntity pending = new TsAgentChatMessageEventEntity();
        pending.setId("confirm-event-1");
        pending.setCreatedAt(new Date(System.currentTimeMillis() - 100L));
        pending.setJson("""
                {
                  "input": {
                    "question": "你对这版角色满意吗？",
                    "options": [
                      {"label": "不满意，重新生成", "value": "REGENERATE"}
                    ]
                  },
                  "output": null,
                  "error": null,
                  "metrics": {}
                }
                """);
        Mockito.when(this.eventService.findLatestPendingInteractiveEvent(
                1001L,
                "role_task_agent",
                "role_confirmation",
                "confirm"
        )).thenReturn(pending);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("optionValue", "REGENERATE");
        resultData.put("selectedOption", Map.of(
                "label", "不满意，重新生成",
                "optionValue", "REGENERATE"
        ));
        this.eventPublisher.publishConfirmEnd(
                this.context,
                "confirm.end",
                "role_confirmation",
                "你对这版角色满意吗？",
                List.of(Map.of("label", "不满意，重新生成", "value", "REGENERATE")),
                resultData
        );

        ArgumentCaptor<Map<String, Object>> completeDataCaptor = mapCaptor();
        Mockito.verify(this.eventService).updateEventResult(
                Mockito.eq("confirm-event-1"),
                Mockito.eq("已选择：不满意，重新生成"),
                Mockito.eq(1),
                completeDataCaptor.capture()
        );
        Map<String, Object> output = castMap(completeDataCaptor.getValue().get("output"));
        Assertions.assertEquals("REGENERATE", output.get("value"));
        Assertions.assertEquals(
                Map.of(
                        "label", "不满意，重新生成",
                        "value", "REGENERATE",
                        "optionValue", "REGENERATE"
                ),
                output.get("selection")
        );
    }

    @Test
    void shouldSendOnlyQuestionAndOptionsInOptionsStartSse() {
        List<Map<String, String>> options = List.of(
                Map.of("label", "现代都市", "optionValue", "MODERN"),
                Map.of("label", "古风仙侠", "optionValue", "XIANXIA")
        );
        this.eventPublisher.publishOptionsStart(
                this.context,
                "options.start",
                "role_style_options",
                "请选择角色风格",
                options
        );

        ArgumentCaptor<Map<String, Object>> payloadCaptor = mapCaptor();
        Mockito.verify(this.sseConnectionManager).sendRaw(
                Mockito.eq("sse-1"),
                Mockito.eq("options.start"),
                payloadCaptor.capture()
        );
        Map<String, Object> payload = payloadCaptor.getValue();
        Assertions.assertEquals(Map.of(
                "question", "请选择角色风格",
                "options", options
        ), payload);
    }

    @Test
    void shouldUpdatePendingOptionsEventAndSendSelectedOptionOnEnd() {
        TsAgentChatMessageEventEntity pending = new TsAgentChatMessageEventEntity();
        pending.setId("options-event-1");
        pending.setCreatedAt(new Date(System.currentTimeMillis() - 100L));
        pending.setJson("""
                {
                  "input": {
                    "question": "请选择角色风格",
                    "options": [
                      {"label": "现代都市", "optionValue": "MODERN"}
                    ]
                  },
                  "output": null,
                  "error": null,
                  "metrics": {}
                }
                """);
        Mockito.when(this.eventService.findLatestPendingInteractiveEvent(
                1001L,
                "role_task_agent",
                "role_style_options",
                "options"
        )).thenReturn(pending);

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("optionValue", "MODERN");
        resultData.put("selectedOption", Map.of(
                "label", "现代都市",
                "optionValue", "MODERN"
        ));
        resultData.put("action", "OPTION_SELECTED");
        resultData.put("reply", "现代都市");
        this.eventPublisher.publishOptionsEnd(
                this.context,
                "options.end",
                "role_style_options",
                "请选择角色风格",
                List.of(Map.of("label", "现代都市", "optionValue", "MODERN")),
                resultData
        );

        ArgumentCaptor<Map<String, Object>> completeDataCaptor = mapCaptor();
        Mockito.verify(this.eventService).updateEventResult(
                Mockito.eq("options-event-1"),
                Mockito.eq("已选择：现代都市"),
                Mockito.eq(1),
                completeDataCaptor.capture()
        );
        Map<String, Object> completeData = completeDataCaptor.getValue();
        Map<String, Object> output = castMap(completeData.get("output"));
        Assertions.assertEquals(
                Map.of("label", "现代都市", "value", "MODERN", "optionValue", "MODERN"),
                output.get("selection")
        );
        Assertions.assertEquals("MODERN", output.get("value"));
        Assertions.assertEquals("OPTION_SELECTED", output.get("action"));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = mapCaptor();
        Mockito.verify(this.sseConnectionManager).sendRaw(
                Mockito.eq("sse-1"),
                Mockito.eq("options.end"),
                payloadCaptor.capture()
        );
        Assertions.assertEquals(
                Map.of("label", "现代都市", "value", "MODERN", "optionValue", "MODERN"),
                payloadCaptor.getValue().get("selectedOption")
        );
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
