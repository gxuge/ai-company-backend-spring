package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsUserImageAsset;
import org.jeecg.modules.system.mapper.TsUserImageAssetMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsUserImageAssetOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsUserImageAssetOwnership {
        String message() default "素材不存在或无权限访问";
    }
    public static final ThreadLocal<TsUserImageAsset> ASSET_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsUserImageAssetMapper tsUserImageAssetMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsUserImageAssetOwnership check) throws Throwable {
        String errorMsg = check == null ? "素材不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        Long userId = IdExtractUtil.toLong(user == null ? null : user.getId());
        Long assetId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsUserImageAsset asset = (assetId == null || userId == null)
                ? null
                : tsUserImageAssetMapper.selectOwned(assetId, userId);
        if (asset == null) {
            throw new JeecgBootException(errorMsg);
        }

        ASSET_CONTEXT.set(asset);
        try {
            return joinPoint.proceed();
        } finally {
            ASSET_CONTEXT.remove();
        }
    }
}
