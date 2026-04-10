package org.jeecg.common.util;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.model.ApiRequestLog;
import org.jeecg.common.model.ApiResponseLog;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 接口调用日志工具：
 * 1. 负责提取请求关键信息（路径、方法、处理器、参数）
 * 2. 负责提取响应关键信息（成功标记、返回码、消息、结果）
 * 3. 负责统一做脱敏与长度截断，避免日志过长或泄漏敏感信息
 */
public final class ApiInvokeLogUtil {

    /** 单条日志文本最大长度，超出后截断 */
    private static final int MAX_TEXT_LEN = 500;
    /** JSON 关键字段脱敏规则 */
    private static final Pattern SENSITIVE_JSON_PATTERN =
        Pattern.compile("\"(?i)(password|token|mobile|idCard)\"\\s*:\\s*\"[^\"]*\"");

    private ApiInvokeLogUtil() {
    }

    /**
     * 组装请求日志对象。
     *
     * @param joinPoint 当前切点
     * @param request   当前 HTTP 请求（可能为空）
     * @return 请求日志数据
     */
    public static ApiRequestLog buildRequestLog(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        String method = request == null ? "UNKNOWN" : request.getMethod();
        String path = request == null ? "UNKNOWN" : request.getRequestURI();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String handler = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String params = safeAndTrim(buildParams(joinPoint.getArgs()));
        return new ApiRequestLog(emphasizePath(path), method, handler, params);
    }

    /**
     * 组装成功响应日志对象。
     *
     * @param result 接口返回值
     * @param costMs 耗时毫秒
     * @return 响应日志数据
     */
    public static ApiResponseLog buildSuccessResponseLog(Object result, long costMs) {
        if (result instanceof Result) {
            Result<?> r = (Result<?>) result;
            return new ApiResponseLog(
                costMs,
                r.isSuccess(),
                String.valueOf(r.getCode()),
                safeAndTrim(String.valueOf(r.getMessage())),
                safeAndTrim(toJsonSafe(r.getResult()))
            );
        }
        return new ApiResponseLog(costMs, true, "200", "OK", safeAndTrim(toJsonSafe(result)));
    }

    /**
     * 组装异常响应日志对象。
     *
     * @param throwable 异常对象
     * @param costMs    耗时毫秒
     * @return 响应日志数据
     */
    public static ApiResponseLog buildErrorResponseLog(Throwable throwable, long costMs) {
        return new ApiResponseLog(
            costMs,
            false,
            "500",
            safeAndTrim(throwable == null ? "unknown error" : throwable.getMessage()),
            throwable == null ? "Throwable" : throwable.getClass().getSimpleName()
        );
    }

    /**
     * 路径显眼化，便于在控制台快速定位目标接口。
     */
    public static String emphasizePath(String path) {
        return ">>>[" + path + "]<<<";
    }

    /**
     * 提取并序列化方法入参，过滤不适合直接打印的参数类型。
     */
    private static String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "{}";
        }
        List<Object> values = new ArrayList<>();
        for (Object arg : args) {
            if (shouldSkipArg(arg)) {
                continue;
            }
            values.add(arg);
        }
        if (values.isEmpty()) {
            return "{}";
        }
        try {
            return JSON.toJSONString(values);
        } catch (Exception e) {
            return values.toString();
        }
    }

    /**
     * 过滤不可序列化、体积大或无必要打印的参数对象。
     */
    private static boolean shouldSkipArg(Object arg) {
        return arg == null
            || arg instanceof ServletRequest
            || arg instanceof ServletResponse
            || arg instanceof MultipartFile
            || arg instanceof MultipartFile[]
            || arg instanceof BindingResult;
    }

    /**
     * 安全 JSON 序列化，失败时降级为字符串。
     */
    private static String toJsonSafe(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    /**
     * 脱敏并截断，避免日志过长或敏感字段泄漏。
     */
    private static String safeAndTrim(String text) {
        if (text == null) {
            return "";
        }
        String masked = SENSITIVE_JSON_PATTERN.matcher(text).replaceAll("\"$1\":\"***\"");
        if (masked.length() <= MAX_TEXT_LEN) {
            return masked;
        }
        return masked.substring(0, MAX_TEXT_LEN) + "...(truncated)";
    }

}
