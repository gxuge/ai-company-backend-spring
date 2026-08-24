package org.jeecg.modules.system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 声明业务方法成功后需要采集的推荐行为事件。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TsBehaviorTrack {

    /** 事件类型。 */
    String eventType();

    /** 固定资源类型。 */
    String resourceType() default "";

    /** 通过 SpEL 动态提取资源类型。 */
    String resourceTypeExpression() default "";

    /** 通过 SpEL 提取用户 ID。 */
    String userIdExpression() default "#user.id";

    /** 通过 SpEL 提取资源 ID。 */
    String resourceIdExpression() default "";

    /** 事件发送条件。 */
    String condition() default "true";
}
