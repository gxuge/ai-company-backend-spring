package org.jeecg.modules.airag.llm.stream;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class ImmediateToolTokenStreamTest {

    @Test
    void shouldAdaptJeecgInternalTokenStreamWhenImmediateToolExists() {
        String toolName = "request_confirmation";
        ToolSpecification specification = buildToolSpecification(toolName);
        ToolExecutor executor = ImmediateToolExecutor.wrap(
                (request, memoryId) -> "{\"success\":true}"
        );
        StreamingChatModel model = Mockito.mock(StreamingChatModel.class);
        ChatMemory memory = newMemory();
        TokenStream source = new org.jeecg.ai.stream.InternalTokenStream(
                model,
                List.of(specification),
                Map.of(toolName, executor),
                memory,
                null
        );

        TokenStream adapted = ImmediateToolTokenStreamAdapter.adapt(
                source,
                Map.of(specification, executor)
        );

        Assertions.assertInstanceOf(ImmediateToolTokenStream.class, adapted);
    }

    @Test
    void shouldCompleteWithoutSecondModelCallForImmediateTool() {
        String toolName = "request_confirmation";
        ToolExecutionRequest toolRequest = buildToolRequest(toolName);
        ChatResponse toolCallResponse = buildResponse(
                AiMessage.from(toolRequest),
                FinishReason.TOOL_EXECUTION
        );
        StreamingChatModel model = Mockito.mock(StreamingChatModel.class);
        AtomicInteger modelCalls = new AtomicInteger();
        Mockito.doAnswer(invocation -> {
            modelCalls.incrementAndGet();
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            handler.onCompleteResponse(toolCallResponse);
            return null;
        }).when(model).chat(
                Mockito.any(dev.langchain4j.model.chat.request.ChatRequest.class),
                Mockito.any(StreamingChatResponseHandler.class)
        );
        AtomicInteger toolCalls = new AtomicInteger();
        ToolExecutor executor = (request, memoryId) -> {
            toolCalls.incrementAndGet();
            return "{\"success\":true,\"data\":{\"question\":\"喜欢吗\"}}";
        };
        ChatMemory memory = newMemory();
        AtomicReference<ChatResponse> completed = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        new ImmediateToolTokenStream(
                model,
                List.of(buildToolSpecification(toolName)),
                Map.of(toolName, executor),
                memory,
                null,
                Set.of(toolName)
        ).onCompleteResponse(completed::set)
                .onError(error::set)
                .start();

        Assertions.assertNull(error.get());
        Assertions.assertSame(toolCallResponse, completed.get());
        Assertions.assertEquals(1, modelCalls.get());
        Assertions.assertEquals(1, toolCalls.get());
        Assertions.assertEquals(3, memory.messages().size());
    }

    @Test
    void shouldRequestModelAgainForNonImmediateTool() {
        String toolName = "generate_content";
        ToolExecutionRequest toolRequest = buildToolRequest(toolName);
        ChatResponse toolCallResponse = buildResponse(
                AiMessage.from(toolRequest),
                FinishReason.TOOL_EXECUTION
        );
        ChatResponse finalResponse = buildResponse(
                AiMessage.from("生成完成"),
                FinishReason.STOP
        );
        StreamingChatModel model = Mockito.mock(StreamingChatModel.class);
        AtomicInteger modelCalls = new AtomicInteger();
        Mockito.doAnswer(invocation -> {
            StreamingChatResponseHandler handler = invocation.getArgument(1);
            if (modelCalls.incrementAndGet() == 1) {
                handler.onCompleteResponse(toolCallResponse);
            } else {
                handler.onCompleteResponse(finalResponse);
            }
            return null;
        }).when(model).chat(
                Mockito.any(dev.langchain4j.model.chat.request.ChatRequest.class),
                Mockito.any(StreamingChatResponseHandler.class)
        );
        ChatMemory memory = newMemory();
        AtomicReference<ChatResponse> completed = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        new ImmediateToolTokenStream(
                model,
                List.of(buildToolSpecification(toolName)),
                Map.of(toolName, (request, memoryId) -> "{\"success\":true}"),
                memory,
                null,
                Set.of("request_confirmation")
        ).onCompleteResponse(completed::set)
                .onError(error::set)
                .start();

        Assertions.assertNull(error.get());
        Assertions.assertSame(finalResponse, completed.get());
        Assertions.assertEquals(2, modelCalls.get());
        Assertions.assertEquals(4, memory.messages().size());
    }

    private ChatMemory newMemory() {
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
        memory.add(UserMessage.from("开始"));
        return memory;
    }

    private ToolExecutionRequest buildToolRequest(String toolName) {
        return ToolExecutionRequest.builder()
                .id("tool-call-1")
                .name(toolName)
                .arguments("{}")
                .build();
    }

    private ToolSpecification buildToolSpecification(String toolName) {
        return ToolSpecification.builder()
                .name(toolName)
                .description("测试工具")
                .build();
    }

    private ChatResponse buildResponse(AiMessage aiMessage, FinishReason finishReason) {
        return ChatResponse.builder()
                .aiMessage(aiMessage)
                .finishReason(finishReason)
                .build();
    }
}
