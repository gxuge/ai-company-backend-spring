package org.jeecg.modules.airag.kb.tool;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.airag.kb.dto.KbRagQuestionDTO;
import org.jeecg.modules.airag.kb.service.IKbRagQaService;
import org.jeecg.modules.airag.kb.vo.KbRagAnswerVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识库RAG问答工具注册器。
 */
@Slf4j
@Component
public class KbRagQaToolRegistrar {
    /**
     * 工具注册中心。
     */
    private final ToolRegistry toolRegistry;

    /**
     * RAG问答服务。
     */
    private final IKbRagQaService kbRagQaService;

    /**
     * 构造函数。
     *
     * @param toolRegistry 工具注册中心
     * @param kbRagQaService RAG问答服务
     */
    public KbRagQaToolRegistrar(ToolRegistry toolRegistry, IKbRagQaService kbRagQaService) {
        this.toolRegistry = toolRegistry;
        this.kbRagQaService = kbRagQaService;
    }

    /**
     * 注册工具。
     */
    @PostConstruct
    public void register() {
        this.toolRegistry.register(buildDefinition());
    }

    /**
     * 构造工具定义。
     *
     * @return 定义
     */
    private ToolDefinition buildDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("kb_rag_qa");
        definition.setDisplayName("知识库RAG问答");
        definition.setDescription("基于知识库和外部知识库执行RAG问答，返回答案、引用和上下文");
        definition.setCategory("rag");
        definition.setTimeoutMs(120000L);
        definition.setRetryable(Boolean.FALSE);
        definition.setInputSchema("""
                {
                  "type":"object",
                  "properties":{
                    "query":{"type":"string","description":"用户问题"},
                    "kb_ids":{"type":"array","items":{"type":"string"}},
                    "external_kb_ids":{"type":"array","items":{"type":"string"}},
                    "search_mode":{"type":"string","enum":["semantic","fulltext","hybrid"]},
                    "answer_mode":{"type":"string","enum":["strict","balanced","creative"]},
                    "metadata_filter":{"type":"object"},
                    "cite_sources":{"type":"boolean"}
                  },
                  "required":["query"]
                }
                """);
        definition.setExecutor(this::execute);
        return definition;
    }

    /**
     * 执行工具。
     *
     * @param context Agent上下文
     * @param request 工具请求
     * @return 工具结果
     */
    private ToolCallResult execute(AgentContext context, ToolCallRequest request) {
        try {
            KbRagQuestionDTO dto = buildDto(context, request);
            KbRagAnswerVO answer = context == null ? this.kbRagQaService.ask(dto) : this.kbRagQaService.ask(context, dto);
            if (context != null) {
                context.putAttribute("kbRagAnswer", answer);
                context.putAttribute("kbRagUsedContext", answer == null ? null : answer.getUsedContext());
                context.setLatestContent(answer == null ? null : answer.getAnswer());
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("answer", answer == null ? null : answer.getAnswer());
            payload.put("citations", answer == null ? List.of() : answer.getCitations());
            payload.put("used_context", answer == null ? List.of() : answer.getUsedContext());
            payload.put("used_queries", answer == null ? List.of() : answer.getUsedQueries());
            payload.put("result_count", answer == null ? 0 : answer.getResultCount());
            payload.put("used_reference_length", answer == null ? 0 : answer.getUsedReferenceLength());
            payload.put("answer_mode", answer == null ? null : answer.getAnswerMode());
            payload.put("debug_info", answer == null ? null : answer.getDebugInfo());
            return ToolCallResult.success("知识库RAG问答完成", payload);
        } catch (Exception ex) {
            log.warn("知识库RAG问答工具执行失败: {}", ex.getMessage(), ex);
            return ToolCallResult.failure(ex.getMessage());
        }
    }

    /**
     * 构造请求对象。
     *
     * @param context 上下文
     * @param request 请求
     * @return DTO
     */
    @SuppressWarnings("unchecked")
    private KbRagQuestionDTO buildDto(AgentContext context, ToolCallRequest request) {
        Map<String, Object> args = request == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.getArguments());
        Object nested = args.get("toolArgs");
        if (nested instanceof Map<?, ?> nestedMap) {
            for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    args.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }

        KbRagQuestionDTO dto = new KbRagQuestionDTO();
        String query = firstText(args, "query", "question", "userInput");
        if (oConvertUtils.isEmpty(query) && context != null) {
            query = context.getUserInput();
        }
        dto.setQuery(query);
        dto.setKbIds(toStringList(args.get("kb_ids"), args.get("kbIds")));
        dto.setExternalKbIds(toStringList(args.get("external_kb_ids"), args.get("externalKbIds")));
        dto.setSearchMode(firstText(args, "search_mode", "searchMode"));
        dto.setAnswerMode(firstText(args, "answer_mode", "answerMode"));
        dto.setTopK(toInteger(args.get("top_k"), args.get("topK")));
        dto.setFinalTopK(toInteger(args.get("final_top_k"), args.get("finalTopK")));
        dto.setReferenceLimit(toInteger(args.get("reference_limit"), args.get("referenceLimit")));
        dto.setUseQueryOptimization(toBoolean(args.get("use_query_optimization"), args.get("useQueryOptimization")));
        dto.setUseRerank(toBoolean(args.get("use_rerank"), args.get("useRerank")));
        dto.setStream(toBoolean(args.get("stream")));
        dto.setCiteSources(toBoolean(args.get("cite_sources"), args.get("citeSources")));
        dto.setMetadataFilter(toMap(args.get("metadata_filter"), args.get("metadataFilter")));
        return dto;
    }

    /**
     * 取第一个文本值。
     *
     * @param args 参数
     * @param keys key
     * @return 文本
     */
    private String firstText(Map<String, Object> args, String... keys) {
        if (args == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = args.get(key);
            if (value != null && oConvertUtils.isNotEmpty(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * 转换字符串列表。
     *
     * @param values 值
     * @return 列表
     */
    private List<String> toStringList(Object... values) {
        Set<String> set = new LinkedHashSet<>();
        if (values != null) {
            for (Object value : values) {
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        if (item != null && oConvertUtils.isNotEmpty(String.valueOf(item))) {
                            set.add(String.valueOf(item));
                        }
                    }
                } else if (value instanceof String str) {
                    if (oConvertUtils.isNotEmpty(str)) {
                        set.add(str);
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 转换整数。
     *
     * @param values 值
     * @return 整数
     */
    private Integer toInteger(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (Exception ignore) {
                // ignore
            }
        }
        return null;
    }

    /**
     * 转换布尔值。
     *
     * @param values 值
     * @return 布尔值
     */
    private Boolean toBoolean(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Boolean bool) {
                return bool;
            }
            String text = String.valueOf(value).trim();
            if ("true".equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    /**
     * 转换Map。
     *
     * @param values 值
     * @return Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object... values) {
        if (values == null) {
            return new LinkedHashMap<>();
        }
        for (Object value : values) {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        result.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                return result;
            }
        }
        return new LinkedHashMap<>();
    }
}
