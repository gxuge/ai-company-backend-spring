package org.jeecg.modules.system.exception.tsactivity;

import org.jeecg.common.api.vo.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将活动业务异常转换为机器可识别响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TsActivityExceptionHandler {

    /** 处理活动业务异常。 */
    @ExceptionHandler(TsActivityBizException.class)
    public Result<Void> handleActivityBizException(TsActivityBizException exception) {
        Result<Void> result = Result.error(HttpStatus.CONFLICT.value(), exception.getMessage());
        result.setErrorCode(exception.getErrorCode().name());
        result.setErrorCategory("ACTIVITY");
        result.setRetryable(exception.isRetryable());
        result.setErrorArgs(exception.getErrorArgs());
        return result;
    }
}
