package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsRoleOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsRoleOwnership {
        String message() default "角色不存在或无权限访问";
    }
    public static final ThreadLocal<TsRole> ROLE_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsRoleMapper tsRoleMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsRoleOwnership check) throws Throwable {
        String errorMsg = check == null ? "角色不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long roleId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsRole role = (roleId == null || userId == null) ? null : tsRoleMapper.selectOwned(roleId, userId);
        if (role == null) {
            throw new JeecgBootException(errorMsg);
        }

        ROLE_CONTEXT.set(role);
        try {
            return joinPoint.proceed();
        } finally {
            ROLE_CONTEXT.remove();
        }
    }
}
