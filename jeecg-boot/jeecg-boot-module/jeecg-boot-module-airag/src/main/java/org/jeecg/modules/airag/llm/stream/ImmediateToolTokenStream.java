package org.jeecg.modules.airag.llm.stream;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 支持 immediate Tool 的流式 Tool 调用循环。
 */
@Slf4j
final class ImmediateToolTokenStream implements TokenStream {

    private final StreamingChatModel model;
    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;
    private final ChatMemory chatMemory;
    private final List<Content> retrievedContents;
    private final Set<String> immediateToolNames;

    private Consumer<String> onPartialResponse;
    private Consumer<PartialThinking> onPartialThinking;
    private Consumer<Throwable> onError;
    private Consumer<List<Content>> onRetrieved;
    private Consumer<ToolExecution> onToolExecuted;
    private Consumer<BeforeToolExecution> beforeToolExecutionHandler;
    private Consumer<ChatResponse> onCompleteResponse;
    private Consumer<ChatResponse> onIntermediateResponse;

    ImmediateToolTokenStream(StreamingChatModel model,
                             List<ToolSpecification> toolSpecifications,
                             Map<String, ToolExecutor> toolExecutors,
                             ChatMemory chatMemory,
                             List<Content> retrievedContents,
                             Set<String> immediateToolNames) {
        this.model = model;
        this.toolSpecifications = toolSpecifications;
        this.toolExecutors = toolExecutors;
        this.chatMemory = chatMemory;
        this.retrievedContents = retrievedContents;
        this.immediateToolNames = immediateToolNames;
    }

    @Override
    public TokenStream onPartialResponse(Consumer<String> handler) {
        this.onPartialResponse = handler;
        return this;
    }

    @Override
    public TokenStream onPartialThinking(Consumer<PartialThinking> handler) {
        this.onPartialThinking = handler;
        return this;
    }

    @Override
    public TokenStream onRetrieved(Consumer<List<Content>> handler) {
        this.onRetrieved = handler;
        return this;
    }

    @Override
    public TokenStream beforeToolExecution(Consumer<BeforeToolExecution> handler) {
        this.beforeToolExecutionHandler = handler;
        return this;
    }

    @Override
    public TokenStream onToolExecuted(Consumer<ToolExecution> handler) {
        this.onToolExecuted = handler;
        return this;
    }

    @Override
    public TokenStream onCompleteResponse(Consumer<ChatResponse> handler) {
        this.onCompleteResponse = handler;
        return this;
    }

    @Override
    public TokenStream onIntermediateResponse(Consumer<ChatResponse> handler) {
        this.onIntermediateResponse = handler;
        return this;
    }

    @Override
    public TokenStream onError(Consumer<Throwable> handler) {
        this.onError = handler;
        return this;
    }

    @Override
    public TokenStream ignoreErrors() {
        return this;
    }

    @Override
    public void start() {
        if (this.onRetrieved != null && this.retrievedContents != null) {
            this.onRetrieved.accept(this.retrievedContents);
        }
        doChat();
    }

    private void doChat() {
        ChatRequest.Builder requestBuilder = ChatRequest.builder()
                .messages(this.chatMemory.messages());
        if (this.toolSpecifications != null && !this.toolSpecifications.isEmpty()) {
            requestBuilder.toolSpecifications(this.toolSpecifications);
        }
        this.model.chat(requestBuilder.build(), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                invokeSafely(onPartialResponse, partialResponse, "partial response");
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                invokeSafely(onPartialThinking, partialThinking, "partial thinking");
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                handleCompleteResponse(response);
            }

            @Override
            public void onError(Throwable error) {
                publishError(error);
            }
        });
    }

    private void handleCompleteResponse(ChatResponse response) {
        try {
            AiMessage aiMessage = response.aiMessage();
            this.chatMemory.add(aiMessage);
            if (!aiMessage.hasToolExecutionRequests()) {
                invokeSafely(this.onCompleteResponse, response, "complete response");
                return;
            }

            invokeSafely(this.onIntermediateResponse, response, "intermediate response");
            boolean allImmediate = true;
            for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                ToolExecutor executor = this.toolExecutors.get(request.name());
                if (executor == null) {
                    publishError(new IllegalStateException(
                            "No ToolExecutor found for tool: " + request.name()
                    ));
                    return;
                }
                String resultText = executeTool(executor, request);
                if (resultText == null || resultText.isBlank()) {
                    publishError(new IllegalArgumentException(
                            "Tool result cannot be null or blank: " + request.name()
                    ));
                    return;
                }
                publishToolExecuted(request, resultText);
                this.chatMemory.add(ToolExecutionResultMessage.from(request, resultText));
                allImmediate = allImmediate && this.immediateToolNames.contains(request.name());
            }

            if (allImmediate) {
                invokeSafely(this.onCompleteResponse, response, "complete response");
                return;
            }
            doChat();
        } catch (RuntimeException ex) {
            publishError(ex);
        }
    }

    private String executeTool(ToolExecutor executor, ToolExecutionRequest request) {
        invokeSafely(
                this.beforeToolExecutionHandler,
                BeforeToolExecution.builder().request(request).build(),
                "before tool execution"
        );
        log.info("[LLMHandler] Executing tool: {}", request.name());
        try {
            return executor.execute(request, this.chatMemory.id());
        } catch (Exception ex) {
            log.error("Tool execution failed: {}", ex.getMessage(), ex);
            return "Tool execution failed: " + ex.getMessage();
        }
    }

    private void publishToolExecuted(ToolExecutionRequest request, String resultText) {
        if (this.onToolExecuted == null) {
            return;
        }
        ToolExecutionResult result = ToolExecutionResult.builder()
                .resultText(resultText)
                .build();
        ToolExecution execution = ToolExecution.builder()
                .request(request)
                .result(result)
                .build();
        invokeSafely(this.onToolExecuted, execution, "tool executed");
    }

    private void publishError(Throwable error) {
        if (this.onError != null) {
            this.onError.accept(error);
        }
    }

    private <T> void invokeSafely(Consumer<T> handler, T value, String callbackName) {
        if (handler == null) {
            return;
        }
        try {
            handler.accept(value);
        } catch (Exception ex) {
            log.warn("Error processing {} callback: {}", callbackName, ex.getMessage());
        }
    }
}
