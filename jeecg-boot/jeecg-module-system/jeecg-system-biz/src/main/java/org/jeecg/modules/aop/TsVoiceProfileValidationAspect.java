package org.jeecg.modules.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
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
public class TsVoiceProfileValidationAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsVoiceProfileExists {
        String message() default "音色不存在";
    }
    public static final ThreadLocal<TsVoiceProfile> VOICE_PROFILE_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsVoiceProfileMapper tsVoiceProfileMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsVoiceProfileExists check) throws Throwable {
        String errorMsg = check == null ? "音色不存在" : check.message();
        Long voiceProfileId = null;
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                voiceProfileId = resolveVoiceProfileId(arg);
                if (voiceProfileId != null) {
                    break;
                }
            }
        }

        TsVoiceProfile profile = (voiceProfileId == null) ? null : tsVoiceProfileMapper.selectActiveById(voiceProfileId);
        if (profile == null) {
            throw new JeecgBootException(errorMsg);
        }
        VOICE_PROFILE_CONTEXT.set(profile);
        try {
            return joinPoint.proceed();
        } finally {
            VOICE_PROFILE_CONTEXT.remove();
        }
    }
    private Long resolveVoiceProfileId(Object arg) {
        Long value = IdExtractUtil.toLong(arg);
        if (value != null) {
            return value;
        }
        value = invokeLongGetter(arg, "getSelectedVoiceProfileId");
        if (value != null) {
            return value;
        }
        value = invokeLongGetter(arg, "getVoiceProfileId");
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
