package org.jeecg.modules.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.modules.system.entity.TsVoiceTag;
import org.jeecg.modules.system.mapper.TsVoiceTagMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
@Aspect
@Component
public class TsVoiceTagValidationAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsVoiceTagExists {
        String message() default "音色标签不存在";
    }
    public static final ThreadLocal<TsVoiceTag> VOICE_TAG_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsVoiceTagMapper tsVoiceTagMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsVoiceTagExists check) throws Throwable {
        String errorMsg = check == null ? "音色标签不存在" : check.message();
        Long tagId = null;
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                tagId = resolveTagId(arg);
                if (tagId != null) {
                    break;
                }
            }
        }

        TsVoiceTag tag = (tagId == null) ? null : tsVoiceTagMapper.selectByTagId(tagId);
        if (tag == null) {
            throw new JeecgBootException(errorMsg);
        }

        VOICE_TAG_CONTEXT.set(tag);
        try {
            return joinPoint.proceed();
        } finally {
            VOICE_TAG_CONTEXT.remove();
        }
    }
    private Long resolveTagId(Object arg) {
        Long value = IdExtractUtil.toLong(arg);
        if (value != null) {
            return value;
        }
        value = invokeLongGetter(arg, "getTagId");
        if (value != null) {
            return value;
        }
        return invokeLongGetter(arg, "getId");
    }
    private Long invokeLongGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return IdExtractUtil.toLong(value);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
