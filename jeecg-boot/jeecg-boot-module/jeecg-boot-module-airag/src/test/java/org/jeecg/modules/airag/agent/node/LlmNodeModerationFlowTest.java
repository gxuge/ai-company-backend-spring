package org.jeecg.modules.airag.agent.node;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.safety.moderation.ModerationAction;
import org.jeecg.modules.airag.safety.moderation.ModerationCategory;
import org.jeecg.modules.airag.safety.moderation.ModerationGuard;
import org.jeecg.modules.airag.safety.moderation.ModerationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class LlmNodeModerationFlowTest {

    @Test
    void shouldPublishOnlyModeratedFinalOutput() throws Exception {
        AgentModelResolver modelResolver = Mockito.mock(AgentModelResolver.class);
        Mockito.when(modelResolver.resolveTextModelId("app-1")).thenReturn("model-1");
        IAIChatHandler chatHandler = Mockito.mock(IAIChatHandler.class);
        Mockito.when(chatHandler.chat(Mockito.eq("model-1"), Mockito.anyList(), Mockito.any()))
                .thenReturn(new CompletingTokenStream("危险增量", "原始输出"));
        AgentEventPublisher eventPublisher = Mockito.mock(AgentEventPublisher.class);
        ModerationGuard moderationGuard = Mockito.mock(ModerationGuard.class);
        ModerationResult allowed = ModerationResult.builder()
                .safe(true)
                .category(ModerationCategory.NONE)
                .score(0.1D)
                .action(ModerationAction.ALLOW)
                .build();
        Mockito.when(moderationGuard.reviewInput(
                Mockito.eq("model-1"),
                Mockito.eq("moderation_test_llm"),
                Mockito.eq("用户输入"),
                Mockito.anyList(),
                Mockito.eq("run-1")
        )).thenReturn(allowed);
        Mockito.when(moderationGuard.isAllowed(allowed)).thenReturn(true);
        Mockito.when(moderationGuard.reviewOutput(
                Mockito.eq("model-1"),
                Mockito.eq("moderation_test_llm"),
                Mockito.eq("原始输出"),
                Mockito.anyList(),
                Mockito.eq("run-1"),
                Mockito.any()
        )).thenReturn("审核后输出");
        TestLlmNode node = new TestLlmNode(modelResolver, chatHandler, eventPublisher);
        AgentContext context = new AgentContext();
        context.setRunId("run-1");
        context.setAppId("app-1");
        context.setUserInput("用户输入");
        context.putAttribute("moderationGuard", moderationGuard);
        context.putAttribute("safetySkillPrompt", "安全规则");

        NodeResult result = node.execute(context);

        Assertions.assertEquals("审核后输出", result.getContent());
        Mockito.verify(eventPublisher, Mockito.never())
                .publishLlmDelta(context, "moderation_test_llm", "危险增量");
        Mockito.verify(eventPublisher)
                .publishLlmDelta(context, "moderation_test_llm", "审核后输出");
    }

    private static class TestLlmNode extends LlmNode {
        TestLlmNode(AgentModelResolver modelResolver,
                    IAIChatHandler chatHandler,
                    AgentEventPublisher eventPublisher) {
            super(
                    "moderation_test_llm",
                    "审核测试节点",
                    definition(),
                    null,
                    modelResolver,
                    chatHandler,
                    eventPublisher
            );
        }

        @Override
        protected Map<String, String> buildPromptVariables(AgentContext context) {
            return Map.of();
        }

        @Override
        protected NodeResult parseResult(String finalText, AgentContext context) {
            return NodeResult.success(finalText);
        }

        private static LlmNodeDefinition definition() {
            LlmNodeDefinition definition = new LlmNodeDefinition();
            definition.setSystemPromptTemplate("业务规则");
            definition.setUserPromptTemplate("用户输入");
            return definition;
        }
    }

    private static class CompletingTokenStream implements TokenStream {
        private final String partial;
        private final String completed;
        private Consumer<String> partialConsumer;
        private Consumer<ChatResponse> completeConsumer;

        CompletingTokenStream(String partial, String completed) {
            this.partial = partial;
            this.completed = completed;
        }

        @Override
        public TokenStream onPartialResponse(Consumer<String> consumer) {
            this.partialConsumer = consumer;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<Content>> consumer) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<ToolExecution> consumer) {
            return this;
        }

        @Override
        public TokenStream onCompleteResponse(Consumer<ChatResponse> consumer) {
            this.completeConsumer = consumer;
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> consumer) {
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            this.partialConsumer.accept(this.partial);
            this.completeConsumer.accept(ChatResponse.builder()
                    .aiMessage(AiMessage.from(this.completed))
                    .finishReason(FinishReason.STOP)
                    .build());
        }
    }
}
