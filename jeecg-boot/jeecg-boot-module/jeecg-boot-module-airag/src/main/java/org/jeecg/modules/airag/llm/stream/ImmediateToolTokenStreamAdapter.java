package org.jeecg.modules.airag.llm.stream;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 JEECG 默认 TokenStream 适配为支持 immediate Tool 的流。
 */
@Slf4j
public final class ImmediateToolTokenStreamAdapter {

    private ImmediateToolTokenStreamAdapter() {
    }

    /**
     * 当参数中存在 immediate Tool 时替换默认流，否则原样返回。
     */
    public static TokenStream adapt(TokenStream tokenStream,
                                    Map<ToolSpecification, ToolExecutor> configuredTools) {
        Set<String> immediateToolNames = resolveImmediateToolNames(configuredTools);
        if (tokenStream == null || immediateToolNames.isEmpty()) {
            return tokenStream;
        }
        try {
            StreamingChatModel model = readField(tokenStream, "model", StreamingChatModel.class);
            List<ToolSpecification> toolSpecifications = readListField(tokenStream, "toolSpecifications");
            Map<String, ToolExecutor> toolExecutors = readMapField(tokenStream, "toolExecutors");
            ChatMemory chatMemory = readField(tokenStream, "chatMemory", ChatMemory.class);
            List<Content> retrievedContents = readListField(tokenStream, "retrievedContents");
            return new ImmediateToolTokenStream(
                    model,
                    toolSpecifications,
                    toolExecutors,
                    chatMemory,
                    retrievedContents,
                    immediateToolNames
            );
        } catch (ReflectiveOperationException | ClassCastException ex) {
            log.warn("Immediate Tool stream adaptation failed; falling back to the default stream", ex);
            return tokenStream;
        }
    }

    private static Set<String> resolveImmediateToolNames(
            Map<ToolSpecification, ToolExecutor> configuredTools) {
        Set<String> names = new LinkedHashSet<>();
        if (configuredTools == null || configuredTools.isEmpty()) {
            return names;
        }
        configuredTools.forEach((specification, executor) -> {
            if (specification != null
                    && ImmediateToolExecutor.isImmediate(executor)
                    && specification.name() != null
                    && !specification.name().isBlank()) {
                names.add(specification.name());
            }
        });
        return names;
    }

    private static <T> T readField(Object target, String fieldName, Class<T> fieldType)
            throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return fieldType.cast(field.get(target));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> readListField(Object target, String fieldName)
            throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return (List<T>) field.get(target);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> readMapField(Object target, String fieldName)
            throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return (Map<K, V>) field.get(target);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
