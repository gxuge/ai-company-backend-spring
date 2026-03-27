package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsChatMessage;
import org.jeecg.modules.system.mapper.TsChatMessageMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsChatMessageOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsChatMessageOwnership {
        String message() default "消息不存在或无权限访问";
    }
    public static final ThreadLocal<TsChatMessage> MESSAGE_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsChatMessageMapper tsChatMessageMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsChatMessageOwnership check) throws Throwable {
        String errorMsg = check == null ? "消息不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long messageId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsChatMessage message = (messageId == null || userId == null)
                ? null
                : tsChatMessageMapper.selectOwnedById(messageId, userId);
        if (message == null) {
            throw new JeecgBootException(errorMsg);
        }

        MESSAGE_CONTEXT.set(message);
        try {
            return joinPoint.proceed();
        } finally {
            MESSAGE_CONTEXT.remove();
        }
    }
}
