package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsStoryOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsStoryOwnership {
        String message() default "故事不存在或无权限访问";
    }
    public static final ThreadLocal<TsStory> STORY_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsStoryMapper tsStoryMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsStoryOwnership check) throws Throwable {
        String errorMsg = check == null ? "故事不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long storyId = IdExtractUtil.extractLongId(joinPoint.getArgs(), "getStoryId");

        TsStory story = (storyId == null || userId == null) ? null : tsStoryMapper.selectOwned(storyId, userId);
        if (story == null) {
            throw new JeecgBootException(errorMsg);
        }

        STORY_CONTEXT.set(story);
        try {
            return joinPoint.proceed();
        } finally {
            STORY_CONTEXT.remove();
        }
    }
}
