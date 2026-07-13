package org.jeecg.modules.system.aspect;

import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.monitor.TsAiLogCollector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class TsAiLogMonitorAspect {

    private static final String TS_ROLE_PREFIX = "/sys/ts-roles/";
    private static final String TS_STORY_PREFIX = "/sys/ts-stories/";
    private static final String TS_CHAT_PREFIX = "/sys/ts-chat-sessions/";
    private static final String TS_AGENT_CHAT_PREFIX = "/sys/ts-agent-chat-sessions/";

    private final TsAiLogCollector collector;

    public TsAiLogMonitorAspect(TsAiLogCollector collector) {
        this.collector = collector;
    }

    @Around("execution(public * org.jeecg.modules.system.controller.TsRoleController.*(..)) || execution(public * org.jeecg.modules.system.controller.TsStoryController.*(..)) || execution(public * org.jeecg.modules.system.controller.TsChatSessionController.*(..)) || execution(public * org.jeecg.modules.system.controller.TsAgentChatSessionController.*(..))")
    public Object aroundTsAiEndpoints(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        String uri = request == null ? null : trimToNull(request.getRequestURI());
        if (!shouldMonitor(uri)) {
            return joinPoint.proceed();
        }

        long startAt = System.currentTimeMillis();
        LoginUser loginUser = currentUser();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String controllerMethod = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String requestPayload = buildRequestPayload(joinPoint.getArgs());
        collector.start(uri,
                request == null ? null : request.getMethod(),
                resolveBizType(uri),
                resolveBizScene(uri),
                controllerMethod,
                requestPayload,
                loginUser == null ? null : loginUser.getId(),
                loginUser == null ? null : loginUser.getUsername());
        collector.appendStep("request_received", "接口入口", "success", step -> {
            step.setRequestPayloadJson(requestPayload);
            step.setExtraInfoJson(buildRequestInfo(uri, request, controllerMethod));
        });

        try {
            Object result = joinPoint.proceed();
            String finalJson = extractResultJson(result);
            collector.appendStep("interface_result", "接口返回", "success", step -> step.setFinalOutputJson(finalJson));
            collector.finishSuccess(finalJson, System.currentTimeMillis() - startAt);
            return result;
        } catch (Throwable ex) {
            collector.appendStep("interface_result", "接口异常", "failed", step -> {
                step.setValidationIssues(ex.getClass().getSimpleName());
                step.setFinalOutputJson(trimToNull(ex.getMessage()));
            });
            collector.finishFailure(ex.getMessage(), System.currentTimeMillis() - startAt);
            throw ex;
        }
    }

    private boolean shouldMonitor(String uri) {
        if (!StringUtils.hasText(uri)) {
            return false;
        }
        boolean roleAi = uri.contains(TS_ROLE_PREFIX) && (uri.contains("/one-click-") || uri.contains("/generate-"));
        boolean storyAi = uri.contains(TS_STORY_PREFIX) && uri.contains("/story-");
        boolean chatAi = uri.contains(TS_CHAT_PREFIX) && (uri.contains("/ai-reply") || uri.contains("/reply-suggestions"));
        boolean agentChatAi = uri.contains(TS_AGENT_CHAT_PREFIX) && uri.contains("/ai-reply");
        return roleAi || storyAi || chatAi || agentChatAi;
    }

    private String resolveBizType(String uri) {
        if (!StringUtils.hasText(uri)) {
            return "unknown";
        }
        if (uri.contains(TS_ROLE_PREFIX)) {
            return "role";
        }
        if (uri.contains(TS_STORY_PREFIX)) {
            return "story";
        }
        if (uri.contains(TS_CHAT_PREFIX)) {
            return "chat";
        }
        if (uri.contains(TS_AGENT_CHAT_PREFIX)) {
            return "agent_chat";
        }
        return "unknown";
    }

    private String resolveBizScene(String uri) {
        if (!StringUtils.hasText(uri)) {
            return null;
        }
        int index = uri.lastIndexOf('/');
        return index >= 0 && index < uri.length() - 1 ? uri.substring(index + 1) : uri;
    }

    private LoginUser currentUser() {
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            return principal instanceof LoginUser ? (LoginUser) principal : null;
        } catch (Exception ex) {
            log.debug("Resolve LoginUser failed: {}", ex.getMessage());
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String buildRequestPayload(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null || arg instanceof HttpServletRequest) {
                continue;
            }
            try {
                return JSONObject.toJSONString(arg);
            } catch (Exception ex) {
                return String.valueOf(arg);
            }
        }
        return null;
    }

    private String buildRequestInfo(String uri, HttpServletRequest request, String controllerMethod) {
        JSONObject info = new JSONObject();
        info.put("endpoint", uri);
        info.put("httpMethod", request == null ? null : request.getMethod());
        info.put("controllerMethod", controllerMethod);
        return info.toJSONString();
    }

    private String extractResultJson(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof Result<?> apiResult) {
            return collector.toJsonString(apiResult.getResult());
        }
        return collector.toJsonString(result);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
