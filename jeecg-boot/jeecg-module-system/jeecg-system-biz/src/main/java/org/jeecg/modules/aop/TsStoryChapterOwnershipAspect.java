package org.jeecg.modules.aop;

import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.IdExtractUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsStoryChapter;
import org.jeecg.modules.system.mapper.TsStoryChapterMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Aspect
@Component
public class TsStoryChapterOwnershipAspect {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CheckTsStoryChapterOwnership {
        String message() default "章节不存在或无权限访问";
    }
    public static final ThreadLocal<TsStoryChapter> CHAPTER_CONTEXT = new ThreadLocal<>();

    @Resource
    private TsStoryChapterMapper tsStoryChapterMapper;
    @Around("@annotation(check)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint, CheckTsStoryChapterOwnership check) throws Throwable {
        String errorMsg = check == null ? "章节不存在或无权限访问" : check.message();

        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userId = user == null ? null : user.getId();
        Long chapterId = IdExtractUtil.extractLongId(joinPoint.getArgs());

        TsStoryChapter chapter = (chapterId == null || userId == null)
                ? null
                : tsStoryChapterMapper.selectOwnedChapter(chapterId, userId);
        if (chapter == null) {
            throw new JeecgBootException(errorMsg);
        }

        CHAPTER_CONTEXT.set(chapter);
        try {
            return joinPoint.proceed();
        } finally {
            CHAPTER_CONTEXT.remove();
        }
    }
}
