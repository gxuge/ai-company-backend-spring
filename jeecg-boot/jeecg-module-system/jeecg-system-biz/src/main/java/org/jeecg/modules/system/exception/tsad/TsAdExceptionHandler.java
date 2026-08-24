package org.jeecg.modules.system.exception.tsad;

import org.jeecg.common.api.vo.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将广告业务异常转换为机器可识别响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TsAdExceptionHandler {

    /** 处理广告业务异常。 */
    @ExceptionHandler(TsAdBizException.class)
    public Result<Void> handleTsAdBizException(TsAdBizException exception) {
        Result<Void> result = Result.error(HttpStatus.CONFLICT.value(), exception.getMessage());
        result.setErrorCode(exception.getErrorCode().name());
        result.setErrorCategory("AD");
        result.setRetryable(false);
        return result;
    }
}
