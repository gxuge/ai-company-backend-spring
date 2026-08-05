package org.jeecg.modules.airag.agent.node;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentConversationMessage;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

class LlmNodeConversationHistoryTest {

    @Test
    void shouldAppendConversationHistoryAsNativeChatMessages() {
        TestLlmNode node = new TestLlmNode(true);
        AgentContext context = new AgentContext();
        context.setAgentCode("role_task_agent");
        context.setConversationMessages(List.of(
                new AgentConversationMessage("1", null, "user", "上一轮用户消息", null, null),
                new AgentConversationMessage("2", "1", "assistant", "上一轮助手回复",
                        "role_task_agent", "role_create_dialog")
        ));

        List<ChatMessage> messages = node.buildTestMessages(context);

        Assertions.assertEquals(4, messages.size());
        Assertions.assertInstanceOf(SystemMessage.class, messages.get(0));
        Assertions.assertInstanceOf(UserMessage.class, messages.get(1));
        Assertions.assertInstanceOf(AiMessage.class, messages.get(2));
        Assertions.assertInstanceOf(UserMessage.class, messages.get(3));
        Assertions.assertEquals("上一轮用户消息", ((UserMessage) messages.get(1)).singleText());
        Assertions.assertEquals("上一轮助手回复", ((AiMessage) messages.get(2)).text());
        Assertions.assertEquals("当前节点任务", ((UserMessage) messages.get(3)).singleText());
    }

    @Test
    void shouldOnlyInjectTurnsAnsweredByCurrentAgent() {
        TestLlmNode node = new TestLlmNode(true);
        AgentContext context = new AgentContext();
        context.setAgentCode("role_task_agent");
        context.setMessageId("5");
        context.setUserInput("她是一个侦探");
        context.setConversationMessages(List.of(
                new AgentConversationMessage("1", null, "user", "我要创建角色", null, null),
                new AgentConversationMessage("2", "1", "assistant", "主Agent建议先描述角色",
                        "main", "ts_agent_deep_agents_main"),
                new AgentConversationMessage("3", null, "user", "我想创建一个美女", null, null),
                new AgentConversationMessage("4", "3", "assistant", "她是什么职业？",
                        "role_task_agent", "role_create_dialog"),
                new AgentConversationMessage("5", null, "user", "她是一个侦探", null, null)
        ));

        List<ChatMessage> messages = node.buildTestMessages(context);

        Assertions.assertEquals(4, messages.size());
        Assertions.assertEquals("我想创建一个美女", ((UserMessage) messages.get(1)).singleText());
        Assertions.assertEquals("她是什么职业？", ((AiMessage) messages.get(2)).text());
        Assertions.assertEquals("当前节点任务", ((UserMessage) messages.get(3)).singleText());
    }

    @Test
    void shouldSkipConversationHistoryWhenNodeDoesNotNeedIt() {
        TestLlmNode node = new TestLlmNode(false);
        AgentContext context = new AgentContext();
        context.setConversationMessages(List.of(
                new AgentConversationMessage("1", "user", "不应传入的历史")
        ));

        List<ChatMessage> messages = node.buildTestMessages(context);

        Assertions.assertEquals(2, messages.size());
        Assertions.assertInstanceOf(SystemMessage.class, messages.get(0));
        Assertions.assertInstanceOf(UserMessage.class, messages.get(1));
        Assertions.assertEquals("当前节点任务", ((UserMessage) messages.get(1)).singleText());
    }

    @Test
    void shouldDeduplicateCurrentUserMessageButKeepItAfterHandoff() {
        TestLlmNode node = new TestLlmNode(true);
        AgentContext mainContext = new AgentContext();
        mainContext.setAgentCode("main");
        mainContext.setMessageId("10");
        mainContext.setUserInput("用户原始输入");
        mainContext.setConversationMessages(List.of(
                new AgentConversationMessage("10", "user", "用户原始输入")
        ));

        Assertions.assertEquals(2, node.buildTestMessages(mainContext).size());

        AgentContext childContext = mainContext.fork("主 Agent 委托任务");
        List<ChatMessage> childMessages = node.buildTestMessages(childContext);

        Assertions.assertEquals(3, childMessages.size());
        Assertions.assertEquals("用户原始输入", ((UserMessage) childMessages.get(1)).singleText());
    }

    @Test
    void shouldKeepTaskDescriptionOnlyInDialogSystemSkill() throws Exception {
        for (Class<?> nodeType : List.of(RoleCreateDialogNode.class, StoryCreateDialogNode.class)) {
            LlmNodeDefinition definition = readDefinition(nodeType);
            String userPrompt = definition.getUserPromptTemplate();

            Assertions.assertNotNull(userPrompt);
            Assertions.assertTrue(userPrompt.contains("{{user_input}}"));
            Assertions.assertFalse(userPrompt.contains("{{task_description}}"));
            Assertions.assertFalse(userPrompt.contains("主 Agent 初始委托"));
        }
    }

    @Test
    void shouldEnableConversationHistoryForInteractiveImageNodes() throws Exception {
        for (Class<?> nodeType : List.of(RoleCreateImageNode.class, StoryCreateBackgroundNode.class)) {
            Assertions.assertTrue(readDefinition(nodeType).isConversationHistoryEnabled());
        }
    }

    @Test
    void shouldPublishToolDefinitionContentTypeOnStart() {
        AgentEventPublisher eventPublisher = Mockito.mock(AgentEventPublisher.class);
        ToolRegistry toolRegistry = Mockito.mock(ToolRegistry.class);
        ToolDefinition toolDefinition = new ToolDefinition();
        toolDefinition.setName("image_tool");
        toolDefinition.setContentType("image");
        Mockito.when(toolRegistry.getDefinition("image_tool")).thenReturn(toolDefinition);
        Mockito.when(toolRegistry.execute(Mockito.any(), Mockito.any()))
                .thenReturn(ToolCallResult.success("图片生成完成", Map.of()));

        ToolCallRequest request = new ToolCallRequest();
        request.setToolName("image_tool");
        request.setArguments(Map.of("prompt", "测试图片"));
        TestLlmNode node = new TestLlmNode(false, eventPublisher);

        node.executeTestTool(new AgentContext(), toolRegistry, request);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = mapCaptor();
        Mockito.verify(eventPublisher).publishToolStart(
                Mockito.any(),
                Mockito.eq("test_llm"),
                Mockito.eq("image_tool"),
                payloadCaptor.capture()
        );
        Assertions.assertEquals("image", payloadCaptor.getValue().get("contentType"));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass((Class) Map.class);
    }

    private static LlmNodeDefinition readDefinition(Class<?> nodeType) throws Exception {
        Method method = nodeType.getDeclaredMethod("buildDefinition");
        method.setAccessible(true);
        return (LlmNodeDefinition) method.invoke(null);
    }

    private static class TestLlmNode extends LlmNode {

        TestLlmNode(boolean conversationHistoryEnabled) {
            this(conversationHistoryEnabled, null);
        }

        TestLlmNode(boolean conversationHistoryEnabled, AgentEventPublisher eventPublisher) {
            super("test_llm", "测试 LLM", buildDefinition(conversationHistoryEnabled),
                    null, null, null, eventPublisher);
        }

        List<ChatMessage> buildTestMessages(AgentContext context) {
            return buildMessages(Map.of(), null, context);
        }

        ToolCallResult executeTestTool(AgentContext context,
                                       ToolRegistry toolRegistry,
                                       ToolCallRequest request) {
            return executeToolWithSse(context, toolRegistry, request);
        }

        @Override
        protected Map<String, String> buildPromptVariables(AgentContext context) {
            return Map.of();
        }

        @Override
        protected NodeResult parseResult(String finalText, AgentContext context) {
            return NodeResult.success(finalText);
        }

        private static LlmNodeDefinition buildDefinition(boolean conversationHistoryEnabled) {
            LlmNodeDefinition definition = new LlmNodeDefinition();
            definition.setSystemPromptTemplate("系统提示");
            definition.setUserPromptTemplate("当前节点任务");
            definition.setConversationHistoryEnabled(conversationHistoryEnabled);
            return definition;
        }
    }
}
