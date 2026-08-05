package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.modules.airag.agent.service.TsAgentChatMessageEventService;
import org.jeecg.modules.airag.agent.sse.SseConnectionManager;
import org.jeecg.modules.airag.agent.sse.SsePayload;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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
        this.eventPublisher = new AgentEventPublisher(this.eventService, this.sseConnectionManager);

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
    void shouldExposeHandoffStatusInAgentEndSse() {
        this.eventPublisher.publishAgentEnd(
                this.context,
                "ts_agent_chat",
                AgentResult.handoff("切换到角色创建子 Agent")
        );

        ArgumentCaptor<SsePayload> payloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("agent.end"),
                payloadCaptor.capture()
        );
        SsePayload payload = payloadCaptor.getValue();
        Assertions.assertEquals("已交还主Agent重新派活", payload.getContent());
        Assertions.assertEquals("HANDOFF", castMap(payload.getData()).get("status"));
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
    void shouldPersistLlmOutputWithoutConversationContext() {
        this.eventPublisher.publishLlmStart(this.context, "dialogNode", "role_dialog_v1");
        this.eventPublisher.updateLlmExecutionMetadata(
                this.context,
                "dialogNode",
                "model-1",
                "DEEPSEEK",
                "deepseek-v4-flash",
                "STOP",
                120,
                30,
                150
        );
        this.eventPublisher.publishLlmError(
                this.context,
                "dialogNode",
                "role_dialog_v1",
                new IllegalStateException("模型调用失败")
        );
        this.eventPublisher.publishLlmEnd(
                this.context,
                "dialogNode",
                "role_dialog_v1",
                false,
                Map.of("content", "不应写入完整输出")
        );

        ArgumentCaptor<Map<String, Object>> dataCaptor = mapCaptor();
        Mockito.verify(this.eventService).saveEvent(
                Mockito.anyString(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("llm"),
                Mockito.eq("deepseek-v4-flash"),
                Mockito.eq("dialogNode"),
                Mockito.eq("llm"),
                Mockito.eq("不应写入完整输出"),
                Mockito.eq(0),
                dataCaptor.capture()
        );

        Map<String, Object> data = dataCaptor.getValue();
        Map<String, Object> input = castMap(data.get("input"));
        Map<String, Object> error = castMap(data.get("error"));
        Map<String, Object> metrics = castMap(data.get("metrics"));
        Assertions.assertEquals("model-1", input.get("modelId"));
        Assertions.assertEquals("DEEPSEEK", input.get("provider"));
        Assertions.assertEquals("deepseek-v4-flash", input.get("modelName"));
        Assertions.assertEquals("role_dialog_v1", input.get("promptCode"));
        Assertions.assertFalse(input.containsKey("messages"));
        Assertions.assertFalse(input.containsKey("prompt"));
        Assertions.assertNull(data.get("output"));
        Assertions.assertEquals("IllegalStateException", error.get("code"));
        Assertions.assertEquals(120, metrics.get("inputTokens"));
        Assertions.assertEquals(30, metrics.get("outputTokens"));
        Assertions.assertEquals(150, metrics.get("totalTokens"));
    }

    @Test
    void shouldPersistSuccessfulLlmStatusAndTokenUsage() {
        this.eventPublisher.publishLlmStart(this.context, "dialogNode", "role_dialog_v1");
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "这是完整回复");
        this.eventPublisher.updateLlmExecutionMetadata(
                this.context,
                "dialogNode",
                "model-1",
                "DEEPSEEK",
                "deepseek-v4-flash",
                "STOP",
                100,
                20,
                120
        );
        this.eventPublisher.publishLlmEnd(
                this.context,
                "dialogNode",
                "role_dialog_v1",
                true,
                Map.of()
        );

        ArgumentCaptor<Map<String, Object>> dataCaptor = mapCaptor();
        Mockito.verify(this.eventService).saveEvent(
                Mockito.anyString(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("llm"),
                Mockito.eq("deepseek-v4-flash"),
                Mockito.eq("dialogNode"),
                Mockito.eq("llm"),
                Mockito.eq("这是完整回复"),
                Mockito.eq(1),
                dataCaptor.capture()
        );
        Map<String, Object> data = dataCaptor.getValue();
        Map<String, Object> output = castMap(data.get("output"));
        Map<String, Object> metrics = castMap(data.get("metrics"));
        Assertions.assertEquals("STOP", output.get("finishReason"));
        Assertions.assertEquals("这是完整回复", output.get("content"));
        Assertions.assertNull(data.get("error"));
        Assertions.assertEquals(120, metrics.get("totalTokens"));
    }

    @Test
    void shouldJoinLlmDeltasInMemoryAndClearBufferOnEnd() {
        this.eventPublisher.publishLlmStart(this.context, "dialogNode", "role_dialog_v1");
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "你好，");
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "我们继续");
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "创建角色。");

        String bufferKey = this.eventPublisher.buildLlmBufferKey(this.context, "dialogNode");
        Assertions.assertEquals("你好，我们继续创建角色。", this.eventPublisher.readBuffer(bufferKey));

        this.eventPublisher.publishLlmEnd(
                this.context,
                "dialogNode",
                "role_dialog_v1",
                true,
                Map.of()
        );

        ArgumentCaptor<SsePayload> payloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("llm.end"),
                payloadCaptor.capture()
        );
        Assertions.assertEquals("你好，我们继续创建角色。", payloadCaptor.getValue().getContent());
        Assertions.assertEquals("", this.eventPublisher.readBuffer(bufferKey));
    }

    @Test
    void shouldPersistLlmSegmentsAroundToolInExecutionOrder() {
        this.context.setEventMessageId("20002");
        this.eventPublisher.publishLlmStart(this.context, "dialogNode", "role_dialog_v1");
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "先整理角色设定。");
        this.eventPublisher.publishToolStart(
                this.context,
                "dialogNode",
                "role_generate_role_image",
                Map.of("toolArguments", Map.of())
        );
        this.eventPublisher.publishToolEnd(
                this.context,
                "dialogNode",
                "role_generate_role_image",
                true,
                "图片生成完成",
                Map.of("toolData", Map.of("imageUrl", "https://example.com/role.png"))
        );
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "图片已经生成好啦。");
        this.eventPublisher.publishLlmEnd(
                this.context,
                "dialogNode",
                "role_dialog_v1",
                true,
                Map.of()
        );

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(this.eventService, Mockito.times(3)).saveEvent(
                Mockito.anyString(),
                Mockito.eq("20002"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                typeCaptor.capture(),
                Mockito.anyString(),
                Mockito.eq("dialogNode"),
                Mockito.anyString(),
                contentCaptor.capture(),
                Mockito.anyInt(),
                Mockito.anyMap()
        );
        Assertions.assertEquals(List.of("llm", "tool", "llm"), typeCaptor.getAllValues());
        Assertions.assertEquals(
                List.of("先整理角色设定。", "图片生成完成", "图片已经生成好啦。"),
                contentCaptor.getAllValues()
        );
    }

    @Test
    void shouldAggregatePersistedAndCurrentLlmTextWhenInterrupted() {
        this.eventPublisher.publishLlmStart(this.context, "dialogNode", "role_dialog_v1");
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "工具调用前的文字");
        this.eventPublisher.publishToolStart(
                this.context,
                "dialogNode",
                "role_generate_role_image",
                Map.of()
        );
        this.eventPublisher.publishLlmDelta(this.context, "dialogNode", "停止前的部分文字");

        this.eventPublisher.publishLlmEnd(
                this.context,
                "dialogNode",
                "role_dialog_v1",
                3,
                Map.of()
        );

        Assertions.assertEquals(
                "工具调用前的文字" + System.lineSeparator() + "停止前的部分文字",
                this.context.getAttribute("interruptedLlmContent")
        );
        Mockito.verify(this.eventService).saveEvent(
                Mockito.anyString(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("llm"),
                Mockito.anyString(),
                Mockito.eq("dialogNode"),
                Mockito.eq("llm"),
                Mockito.eq("停止前的部分文字"),
                Mockito.eq(3),
                Mockito.anyMap()
        );
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
    void shouldDefaultToolStartContentTypeToProgress() {
        this.eventPublisher.publishToolStart(
                this.context,
                "role_create_dialog",
                "role_generate_role",
                Map.of()
        );

        ArgumentCaptor<SsePayload> payloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("tool.start"),
                payloadCaptor.capture()
        );
        Assertions.assertEquals("progress", payloadCaptor.getValue().getContentType());
    }

    @Test
    void shouldUpdateSameEventWhenAsyncToolCompletes() {
        String eventId = "async-event-1";
        Map<String, Object> arguments = Map.of(
                "transferData",
                Map.of(
                        "roleName", "林夏",
                        "gender", "女性",
                        "occupation", "骑士",
                        "backgroundStory", "王城守卫"
                )
        );
        Map<String, Object> startPayload = new LinkedHashMap<>();
        startPayload.put("eventId", eventId);
        startPayload.put("taskId", "async-task-1");
        startPayload.put("async", Boolean.TRUE);
        startPayload.put("toolArguments", arguments);

        this.eventPublisher.publishAsyncToolStart(
                this.context,
                eventId,
                "role_create_dialog",
                "role_generate_complete",
                startPayload
        );
        AgentContext asyncContext = this.context.fork(this.context.getUserInput());
        this.eventPublisher.publishAsyncToolEnd(
                asyncContext,
                eventId,
                "role_create_dialog",
                "role_generate_complete",
                "完整角色生成完成",
                Map.of(
                        "eventId", eventId,
                        "taskId", "async-task-1",
                        "async", Boolean.TRUE,
                        "toolArguments", arguments,
                        "toolData", Map.of("roleId", 88L)
                )
        );

        ArgumentCaptor<Map<String, Object>> pendingDataCaptor = mapCaptor();
        Mockito.verify(this.eventService).saveEvent(
                Mockito.eq(eventId),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("tool"),
                Mockito.eq("role_generate_complete"),
                Mockito.eq("role_create_dialog"),
                Mockito.eq("tool"),
                Mockito.eq("开始调用 role_generate_complete"),
                Mockito.eq(2),
                pendingDataCaptor.capture()
        );
        Assertions.assertEquals(Boolean.TRUE, pendingDataCaptor.getValue().get("async"));
        ArgumentCaptor<Map<String, Object>> completeDataCaptor = mapCaptor();
        Mockito.verify(this.eventService).updateEventResult(
                Mockito.eq(eventId),
                Mockito.eq("完整角色生成完成"),
                Mockito.eq(1),
                completeDataCaptor.capture()
        );
        Assertions.assertEquals(Boolean.TRUE, completeDataCaptor.getValue().get("async"));
        Map<String, Object> output = castMap(completeDataCaptor.getValue().get("output"));
        Assertions.assertEquals(Map.of("roleId", 88L), output.get("result"));
        Mockito.verify(this.sseConnectionManager).retain("sse-1");
        Mockito.verify(this.sseConnectionManager).release("sse-1");

        ArgumentCaptor<SsePayload> payloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager, Mockito.times(2)).send(
                Mockito.eq("sse-1"),
                Mockito.anyString(),
                payloadCaptor.capture()
        );
        Assertions.assertEquals(eventId, payloadCaptor.getAllValues().get(0).getEventId());
        Assertions.assertEquals(eventId, payloadCaptor.getAllValues().get(1).getEventId());
        Assertions.assertTrue(payloadCaptor.getAllValues().get(0).getAsync());
        Assertions.assertTrue(payloadCaptor.getAllValues().get(1).getAsync());
    }

    @Test
    void shouldExposeRoleConfirmationDataInToolEndSse() {
        Map<String, Object> transferData = Map.of(
                "roleName", "林夏",
                "gender", "女性",
                "occupation", "骑士",
                "backgroundStory", "王城守卫"
        );
        Map<String, Object> toolData = new LinkedHashMap<>();
        toolData.put("question", "你对这版角色满意吗？");
        toolData.put("interactionId", "interaction-1");
        toolData.put("interactionType", "confirm");
        toolData.put("contextRef", "transferDataJson");
        toolData.put("status", "PENDING");
        toolData.put("suspendRun", true);
        toolData.put("transferData", transferData);
        toolData.put("options", List.of(
                Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                Map.of("label", "不满意，重新生成", "value", "REGENERATE")
        ));

        this.eventPublisher.publishToolStart(
                this.context,
                "role_create_dialog",
                "role_request_confirmation",
                Map.of()
        );
        this.eventPublisher.publishToolEnd(
                this.context,
                "role_create_dialog",
                "role_request_confirmation",
                true,
                "你对这版角色满意吗？",
                Map.of("toolData", toolData)
        );

        ArgumentCaptor<SsePayload> payloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("tool.end"),
                payloadCaptor.capture()
        );
        SsePayload payload = payloadCaptor.getValue();
        Assertions.assertEquals("options", payload.getContentType());
        Assertions.assertEquals("interaction-1", payload.getInteractionId());
        Assertions.assertEquals("你对这版角色满意吗？", payload.getQuestion());
        Assertions.assertEquals(2, payload.getOptions().size());
    }

    @Test
    void shouldExposeAndPersistFlatImageToolFields() {
        Map<String, Object> arguments = Map.of(
                "roleName", "林夏",
                "occupation", "骑士"
        );
        this.eventPublisher.publishToolStart(
                this.context,
                "role_create_image",
                "role_generate_role_image",
                Map.of(
                        "toolArguments", arguments,
                        "contentType", "image"
                )
        );
        Map<String, Object> endPayload = new LinkedHashMap<>();
        endPayload.put("toolArguments", arguments);
        endPayload.put("contentType", "image");
        endPayload.put("resourceType", "role_image");
        endPayload.put("imageUrl", "https://example.com/role.png");
        endPayload.put("promptCode", "role_image_generate");
        endPayload.put("promptVersion", "v1");
        this.eventPublisher.publishToolEnd(
                this.context,
                "role_create_image",
                "role_generate_role_image",
                true,
                "已生成角色形象",
                endPayload
        );

        ArgumentCaptor<Map<String, Object>> dataCaptor = mapCaptor();
        Mockito.verify(this.eventService).saveEvent(
                Mockito.anyString(),
                Mockito.eq("10001"),
                Mockito.eq(1001L),
                Mockito.isNull(),
                Mockito.eq("tool"),
                Mockito.eq("role_generate_role_image"),
                Mockito.eq("role_create_image"),
                Mockito.eq("tool"),
                Mockito.eq("已生成角色形象"),
                Mockito.eq(1),
                dataCaptor.capture()
        );
        Map<String, Object> output = castMap(dataCaptor.getValue().get("output"));
        Assertions.assertEquals("image", output.get("contentType"));
        Assertions.assertEquals("role_image", output.get("resourceType"));
        Assertions.assertEquals("https://example.com/role.png", output.get("imageUrl"));
        Assertions.assertEquals("role_image_generate", output.get("promptCode"));
        Assertions.assertEquals("v1", output.get("promptVersion"));
        Assertions.assertFalse(output.containsKey("result"));

        ArgumentCaptor<SsePayload> startPayloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("tool.start"),
                startPayloadCaptor.capture()
        );
        Assertions.assertEquals("image", startPayloadCaptor.getValue().getContentType());

        ArgumentCaptor<SsePayload> payloadCaptor = ArgumentCaptor.forClass(SsePayload.class);
        Mockito.verify(this.sseConnectionManager).send(
                Mockito.eq("sse-1"),
                Mockito.eq("tool.end"),
                payloadCaptor.capture()
        );
        SsePayload payload = payloadCaptor.getValue();
        Assertions.assertEquals("image", payload.getContentType());
        Assertions.assertEquals("role_image", payload.getResourceType());
        Assertions.assertEquals("https://example.com/role.png", payload.getImageUrl());
        Assertions.assertEquals("role_image_generate", payload.getPromptCode());
        Assertions.assertEquals("v1", payload.getPromptVersion());
        Assertions.assertNull(payload.getResult());
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
