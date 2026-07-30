package org.jeecg.modules.aop;

import jakarta.annotation.Resource;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.modules.system.entity.TsDraft;
import org.jeecg.modules.system.mapper.TsDraftMapper;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 统一草稿归属校验切面。
 */
@Aspect
@Component
public class TsDraftOwnershipAspect {

    /**
     * 校验当前登录用户是否拥有指定草稿。
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsDraftOwnership {

        /** 校验失败时的业务提示。 */
        String message() default "草稿不存在或无权限访问";
    }

    /** 当前调用链已完成归属校验的草稿上下文。 */
    public static final ThreadLocal<TsDraft> DRAFT_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsDraftMapper tsDraftMapper;

    /**
     * 从方法参数提取草稿 ID，并按当前登录用户查询有效草稿。
     *
     * @param joinPoint 被拦截的方法
     * @param check 归属校验注解
     * @return 原方法执行结果
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsDraftOwnership check) throws Throwable {
        String errorMessage = check == null ? "草稿不存在或无权限访问" : check.message();
        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long draftId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsDraft draft = draftId == null || userId == null
                ? null
                : tsDraftMapper.selectOwned(draftId, userId);
        if (draft == null) {
            throw new JeecgBootException(errorMessage);
        }

        DRAFT_CONTEXT.set(draft);
        try {
            return joinPoint.proceed();
        } finally {
            DRAFT_CONTEXT.remove();
        }
    }
}
