package org.jeecg.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.model.ApiRequestLog;
import org.jeecg.common.model.ApiResponseLog;
import org.jeecg.common.util.ApiInvokeLogUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口调用日志切面：
 * 只负责流程编排，具体提取/脱敏/截断逻辑下沉到 common.util。
 */
@Slf4j
@Aspect
@Component
public class ApiInvokeLogAspect {

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object aroundRestController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        ApiRequestLog reqLog = ApiInvokeLogUtil.buildRequestLog(joinPoint, currentRequest());

        log.info("[API_REQ] endpoint={} method={} handler={} params={}",
            reqLog.getEndpoint(), reqLog.getMethod(), reqLog.getHandler(), reqLog.getParams());

        try {
            Object result = joinPoint.proceed();
            ApiResponseLog resLog = ApiInvokeLogUtil.buildSuccessResponseLog(result, System.currentTimeMillis() - start);
            log.info("[API_RES] endpoint={} method={} costMs={} success={} code={} msg={} result={}",
                reqLog.getEndpoint(), reqLog.getMethod(), resLog.getCostMs(), resLog.isSuccess(),
                resLog.getCode(), resLog.getMsg(), resLog.getResult());
            return result;
        } catch (Throwable ex) {
            ApiResponseLog resLog = ApiInvokeLogUtil.buildErrorResponseLog(ex, System.currentTimeMillis() - start);
            log.error("[API_RES] endpoint={} method={} costMs={} success={} code={} msg={} result={}",
                reqLog.getEndpoint(), reqLog.getMethod(), resLog.getCostMs(), resLog.isSuccess(),
                resLog.getCode(), resLog.getMsg(), resLog.getResult());
            throw ex;
        }
    }

    /**
     * 获取当前请求上下文，非 HTTP 场景返回 null。
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
