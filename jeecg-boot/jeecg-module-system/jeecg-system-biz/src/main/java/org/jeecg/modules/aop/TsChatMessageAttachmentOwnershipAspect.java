package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;
import org.jeecg.modules.system.mapper.TsChatMessageAttachmentMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsChatMessageAttachmentOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsChatMessageAttachmentOwnership {
        String message() default "消息附件不存在或无权限访问";
    }
    public static final ThreadLocal<TsChatMessageAttachment> ATTACHMENT_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsChatMessageAttachmentMapper tsChatMessageAttachmentMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsChatMessageAttachmentOwnership check) throws Throwable {
        String errorMsg = check == null ? "消息附件不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long attachmentId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsChatMessageAttachment attachment = (attachmentId == null || userId == null)
                ? null
                : tsChatMessageAttachmentMapper.selectOwnedById(attachmentId, userId);
        if (attachment == null) {
            throw new JeecgBootException(errorMsg);
        }

        ATTACHMENT_CONTEXT.set(attachment);
        try {
            return joinPoint.proceed();
        } finally {
            ATTACHMENT_CONTEXT.remove();
        }
    }
}