package org.jeecg.modules.system.exception.tspoints;

import org.jeecg.common.api.vo.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将积分业务异常转换为统一机器可识别响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TsPointsExceptionHandler {

    /** 处理积分业务异常。 */
    @ExceptionHandler(TsPointsBizException.class)
    public Result<Void> handlePointsBizException(TsPointsBizException exception) {
        Result<Void> result = Result.error(HttpStatus.CONFLICT.value(), exception.getMessage());
        result.setErrorCode(exception.getErrorCode().name());
        result.setErrorCategory("POINTS");
        result.setRetryable(exception.isRetryable());
        result.setErrorArgs(exception.getErrorArgs());
        return result;
    }
}
