package org.jeecg.modules.system.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.config.AiRagConfigBean;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级 ts MCP Server。
 * 当前仅暴露只读公开角色列表，便于线上接入验证。
 */
@Slf4j
@RestController
@RequestMapping("/ts/mcp")
@Tag(name = "TS MCP Server")
public class TsMcpServerController {

    private static final String API_KEY_HEADER = "X-MCP-KEY";
    private static final String TOOL_LIST_PUBLIC_TS_ROLES = "list_public_ts_roles";

    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    private final TsRoleMapper tsRoleMapper;
    private final AiRagConfigBean aiRagConfigBean;

    public TsMcpServerController(TsRoleMapper tsRoleMapper, AiRagConfigBean aiRagConfigBean) {
        this.tsRoleMapper = tsRoleMapper;
        this.aiRagConfigBean = aiRagConfigBean;
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @IgnoreAuth
    @Operation(summary = "TS MCP SSE 连接端点")
    public SseEmitter sse(HttpServletRequest request,
                          @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey) {
        ensureServerEnabled();
        validateApiKey(apiKey);
        String clientId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitters.put(clientId, emitter);
        emitter.onCompletion(() -> sseEmitters.remove(clientId));
        emitter.onTimeout(() -> sseEmitters.remove(clientId));
        emitter.onError(e -> sseEmitters.remove(clientId));
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String messageEndpoint = baseUrl + request.getContextPath() + "/ts/mcp/message?sessionId=" + clientId;
            emitter.send(SseEmitter.event().name("endpoint").data(messageEndpoint));
        } catch (IOException e) {
            log.warn("[TS MCP] 发送 SSE endpoint 事件失败: {}", e.getMessage());
        }
        return emitter;
    }

    @PostMapping("/sse")
    @IgnoreAuth
    @Operation(summary = "TS MCP Streamable HTTP 端点")
    public void ssePost(@RequestBody String body,
                        @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                        HttpServletResponse response) throws IOException {
        ensureServerEnabled();
        validateApiKey(apiKey);
        handleJsonRpcRequest(body, response);
    }

    @PostMapping("/message")
    @IgnoreAuth
    @Operation(summary = "TS MCP 消息处理")
    public void handleMessage(@RequestParam(required = false) String sessionId,
                              @RequestBody String body,
                              @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                              HttpServletResponse response) throws IOException {
        ensureServerEnabled();
        validateApiKey(apiKey);
        log.info("[TS MCP] 收到消息 sessionId={}, body={}", sessionId, body);
        handleJsonRpcRequest(body, response);
    }

    @GetMapping("/info")
    @IgnoreAuth
    @Operation(summary = "TS MCP Server 说明")
    public Map<String, Object> info(HttpServletRequest request) {
        ensureServerEnabled();
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        boolean apiKeyEnabled = StringUtils.hasText(aiRagConfigBean.getMcpServer().getApiKey());
        return Map.of(
                "success", true,
                "message", "TS MCP Server 已启用",
                "sseUrl", baseUrl + "/ts/mcp/sse",
                "messageUrl", baseUrl + "/ts/mcp/message",
                "authHeader", API_KEY_HEADER,
                "apiKeyEnabled", apiKeyEnabled,
                "tools", List.of(
                        Map.of(
                                "name", TOOL_LIST_PUBLIC_TS_ROLES,
                                "description", "获取公开角色列表",
                                "params", "keyword(可选), limit(可选, 默认10, 最大50)"
                        )
                )
        );
    }

    private void handleJsonRpcRequest(String body, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        try {
            JSONObject request = JSON.parseObject(body);
            String method = request.getString("method");
            Object id = request.get("id");
            JSONObject params = request.getJSONObject("params");
            if (id == null) {
                writer.write("{}");
                writer.flush();
                return;
            }

            Map<String, Object> jsonRpcResponse = new LinkedHashMap<>();
            jsonRpcResponse.put("jsonrpc", "2.0");
            jsonRpcResponse.put("id", id);
            try {
                Object result = switch (method) {
                    case "initialize" -> handleInitialize();
                    case "initialized", "notifications/initialized" -> Map.of();
                    case "tools/list" -> handleToolsList();
                    case "tools/call" -> handleToolsCall(params);
                    case "ping" -> Map.of();
                    default -> throw new RuntimeException("未知方法: " + method);
                };
                jsonRpcResponse.put("result", result);
            } catch (Exception e) {
                log.error("[TS MCP] 处理请求失败", e);
                jsonRpcResponse.put("error", Map.of("code", -32603, "message", e.getMessage()));
            }
            writer.write(JSON.toJSONString(jsonRpcResponse));
        } catch (Exception e) {
            log.error("[TS MCP] 解析请求失败", e);
            writer.write("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}");
        }
        writer.flush();
    }

    private Map<String, Object> handleInitialize() {
        return Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of("name", "ts-mcp-server", "version", "1.0.0")
        );
    }

    private Map<String, Object> handleToolsList() {
        return Map.of("tools", List.of(buildPublicRolesToolDefinition()));
    }

    private Map<String, Object> handleToolsCall(JSONObject params) {
        if (params == null) {
            throw new RuntimeException("缺少 tools/call 参数");
        }
        String toolName = params.getString("name");
        JSONObject arguments = params.getJSONObject("arguments");
        if (!TOOL_LIST_PUBLIC_TS_ROLES.equals(toolName)) {
            throw new RuntimeException("未知工具: " + toolName);
        }
        Map<String, Object> result = listPublicRoles(arguments);
        return Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", JSON.toJSONString(result)
                )),
                "structuredContent", result
        );
    }

    private Map<String, Object> listPublicRoles(JSONObject arguments) {
        int defaultLimit = positiveOrDefault(aiRagConfigBean.getMcpServer().getDefaultRoleListLimit(), 10);
        int maxLimit = positiveOrDefault(aiRagConfigBean.getMcpServer().getMaxRoleListLimit(), 50);
        int requestedLimit = arguments == null ? defaultLimit : arguments.getIntValue("limit");
        int limit = requestedLimit <= 0 ? defaultLimit : Math.min(requestedLimit, maxLimit);
        String keyword = arguments == null ? null : trimToNull(arguments.getString("keyword"));

        QueryWrapper<TsRole> wrapper = new QueryWrapper<>();
        wrapper.select("id", "role_name", "role_subtitle", "avatar_url", "gender", "occupation", "intro_text", "updated_at");
        wrapper.eq("is_public", 1);
        wrapper.and(q -> q.isNull("status").or().ne("status", 0));
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like("role_name", keyword).or().like("role_subtitle", keyword).or().like("intro_text", keyword));
        }
        wrapper.orderByDesc("updated_at").orderByDesc("id");
        wrapper.last("LIMIT " + limit);

        List<TsRole> roles = tsRoleMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (TsRole role : roles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", role.getId());
            item.put("roleName", role.getRoleName());
            item.put("roleSubtitle", role.getRoleSubtitle());
            item.put("avatarUrl", role.getAvatarUrl());
            item.put("gender", role.getGender());
            item.put("occupation", role.getOccupation());
            item.put("introText", role.getIntroText());
            item.put("updatedAt", role.getUpdatedAt());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", TOOL_LIST_PUBLIC_TS_ROLES);
        result.put("count", items.size());
        result.put("limit", limit);
        result.put("keyword", keyword);
        result.put("items", items);
        return result;
    }

    private Map<String, Object> buildPublicRolesToolDefinition() {
        return Map.of(
                "name", TOOL_LIST_PUBLIC_TS_ROLES,
                "description", "获取公开角色列表，仅返回安全摘要字段",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "可选：按角色名/副标题/简介模糊搜索"),
                                "limit", Map.of("type", "integer", "description", "可选：返回数量上限")
                        )
                )
        );
    }

    private void ensureServerEnabled() {
        if (!aiRagConfigBean.getMcpServer().isEnabled()) {
            throw new RuntimeException("TS MCP Server 未启用");
        }
    }

    private void validateApiKey(String apiKey) {
        String configured = trimToNull(aiRagConfigBean.getMcpServer().getApiKey());
        if (!StringUtils.hasText(configured)) {
            return;
        }
        if (!configured.equals(trimToNull(apiKey))) {
            throw new RuntimeException("MCP API Key 无效");
        }
    }

    private int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }
}
