package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.mapper.TsChatSessionMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsChatSessionOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsChatSessionOwnership {
        String message() default "会话不存在或无权限访问";
    }
    public static final ThreadLocal<TsChatSession> SESSION_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsChatSessionMapper tsChatSessionMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsChatSessionOwnership check) throws Throwable {
        String errorMsg = check == null ? "会话不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long sessionId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsChatSession session = (sessionId == null || userId == null)
                ? null
                : tsChatSessionMapper.selectOwnedById(sessionId, userId);
        if (session == null) {
            throw new JeecgBootException(errorMsg);
        }

        SESSION_CONTEXT.set(session);
        try {
            return joinPoint.proceed();
        } finally {
            SESSION_CONTEXT.remove();
        }
    }
}
