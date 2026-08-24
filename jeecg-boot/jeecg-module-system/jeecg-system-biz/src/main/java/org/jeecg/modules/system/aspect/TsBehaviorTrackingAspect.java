package org.jeecg.modules.system.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.annotation.TsBehaviorTrack;
import org.jeecg.modules.system.behavior.TsBehaviorEventCoordinator;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 将成功的后端业务方法转换为可信推荐行为事件。 */
@Slf4j
@Aspect
@Component
public class TsBehaviorTrackingAspect {

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();
    private final TsBehaviorEventCoordinator eventCoordinator;
    private final ObjectMapper objectMapper;

    /** 注入事件协调器和 JSON 序列化器。 */
    public TsBehaviorTrackingAspect(
            TsBehaviorEventCoordinator eventCoordinator,
            ObjectMapper objectMapper) {
        this.eventCoordinator = eventCoordinator;
        this.objectMapper = objectMapper;
    }

    /** 业务成功返回后解析注解并安排事件发布。 */
    @Around("@annotation(behaviorTrack)")
    public Object track(
            ProceedingJoinPoint joinPoint,
            TsBehaviorTrack behaviorTrack) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof Result<?> apiResult && !apiResult.isSuccess()) {
            return result;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = AopUtils.getMostSpecificMethod(
                    signature.getMethod(), joinPoint.getTarget().getClass());
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(),
                    method,
                    joinPoint.getArgs(),
                    parameterNameDiscoverer);
            context.setVariable("result", result);
            if (!evaluateCondition(behaviorTrack.condition(), context)) {
                return result;
            }
            TsBehaviorEventMessage event = buildEvent(behaviorTrack, method, context);
            if (!StringUtils.hasText(event.getUserId())
                    || !StringUtils.hasText(event.getResourceType())) {
                log.warn(
                        "跳过字段不完整的后端行为事件，method={}, eventType={}",
                        method.getName(),
                        behaviorTrack.eventType());
                return result;
            }
            eventCoordinator.publishAfterCommit(event);
        } catch (RuntimeException exception) {
            log.warn(
                    "后端行为埋点解析失败，method={}, eventType={}",
                    joinPoint.getSignature().toShortString(),
                    behaviorTrack.eventType(),
                    exception);
        }
        return result;
    }

    /** 构造统一的服务端行为消息。 */
    private TsBehaviorEventMessage buildEvent(
            TsBehaviorTrack annotation,
            Method method,
            MethodBasedEvaluationContext context) {
        Date now = new Date();
        return new TsBehaviorEventMessage()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(annotation.eventType())
                .setEventVersion(1)
                .setUserId(evaluateText(annotation.userIdExpression(), context))
                .setSessionId("backend")
                .setResourceType(resolveResourceType(annotation, context))
                .setResourceId(evaluateText(annotation.resourceIdExpression(), context))
                .setPlatform("SERVER")
                .setPropertiesJson(buildProperties(method))
                .setOccurredAt(now)
                .setReceivedAt(now);
    }

    /** 解析固定或动态资源类型。 */
    private String resolveResourceType(
            TsBehaviorTrack annotation,
            MethodBasedEvaluationContext context) {
        if (StringUtils.hasText(annotation.resourceTypeExpression())) {
            return evaluateText(annotation.resourceTypeExpression(), context);
        }
        return trimToNull(annotation.resourceType());
    }

    /** 计算事件条件。 */
    private boolean evaluateCondition(
            String expression,
            MethodBasedEvaluationContext context) {
        Boolean value = expressionParser.parseExpression(expression).getValue(context, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    /** 计算字符串表达式并统一转换为空值。 */
    private String evaluateText(
            String expression,
            MethodBasedEvaluationContext context) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    /** 构建后端事件的最小扩展属性。 */
    private String buildProperties(Method method) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("source", "backend");
        properties.put("operation", method.getDeclaringClass().getSimpleName() + "." + method.getName());
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("后端行为扩展属性序列化失败", exception);
        }
    }

    /** 去除空白并将空字符串转换为 null。 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
