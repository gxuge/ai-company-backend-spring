package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsRoleImageGenerateRecord;
import org.jeecg.modules.system.mapper.TsRoleImageGenerateRecordMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsRoleImageGenerateRecordOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsRoleImageGenerateRecordOwnership {
        String message() default "生成记录不存在或无权限访问";
    }
    public static final ThreadLocal<TsRoleImageGenerateRecord> RECORD_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsRoleImageGenerateRecordMapper tsRoleImageGenerateRecordMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsRoleImageGenerateRecordOwnership check) throws Throwable {
        String errorMsg = check == null ? "生成记录不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long recordId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsRoleImageGenerateRecord record = (recordId == null || userId == null)
                ? null
                : tsRoleImageGenerateRecordMapper.selectOwned(recordId, userId);
        if (record == null) {
            throw new JeecgBootException(errorMsg);
        }

        RECORD_CONTEXT.set(record);
        try {
            return joinPoint.proceed();
        } finally {
            RECORD_CONTEXT.remove();
        }
    }
}
