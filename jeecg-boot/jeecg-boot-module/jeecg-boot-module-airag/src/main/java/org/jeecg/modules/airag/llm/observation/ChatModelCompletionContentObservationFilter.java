package org.jeecg.modules.airag.llm.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.content.Content;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps prompt/completion texts to OTel attributes so Langfuse can display them as input/output.
 */
@Component
public class ChatModelCompletionContentObservationFilter implements ObservationFilter {

    private static final String PROMPT_KEY = "gen_ai.prompt";
    private static final String COMPLETION_KEY = "gen_ai.completion";

    @Override
    public Observation.Context map(Observation.Context context) {
        if (!(context instanceof ChatModelObservationContext chatContext)) {
            return context;
        }

        String prompt = joinNonBlank(extractPromptTexts(chatContext));
        if (StringUtils.hasText(prompt)) {
            chatContext.addHighCardinalityKeyValue(KeyValue.of(PROMPT_KEY, prompt));
        }

        String completion = joinNonBlank(extractCompletionTexts(chatContext));
        if (StringUtils.hasText(completion)) {
            chatContext.addHighCardinalityKeyValue(KeyValue.of(COMPLETION_KEY, completion));
        }

        return chatContext;
    }

    private List<String> extractPromptTexts(ChatModelObservationContext context) {
        if (context.getRequest() == null || CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return List.of();
        }

        List<String> texts = new ArrayList<>();
        for (Object instruction : context.getRequest().getInstructions()) {
            String text = extractText(instruction);
            if (StringUtils.hasText(text)) {
                texts.add(text);
            }
        }
        return texts;
    }

    private List<String> extractCompletionTexts(ChatModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return List.of();
        }

        List<String> texts = new ArrayList<>();
        for (Object generation : context.getResponse().getResults()) {
            Object output = invokeNoArg(generation, "getOutput");
            String text = extractText(output);
            if (StringUtils.hasText(text)) {
                texts.add(text);
            }
        }
        return texts;
    }

    private String extractText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Content content && StringUtils.hasText(content.getText())) {
            return content.getText();
        }
        Object text = invokeNoArg(value, "getText");
        if (text instanceof String textStr && StringUtils.hasText(textStr)) {
            return textStr;
        }
        Object content = invokeNoArg(value, "getContent");
        if (content instanceof String contentStr && StringUtils.hasText(contentStr)) {
            return contentStr;
        }
        return "";
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String joinNonBlank(List<String> items) {
        if (CollectionUtils.isEmpty(items)) {
            return "";
        }
        return String.join("\n\n", items);
    }
}

