package org.jeecg.modules.system.exception.tsreward;

import org.jeecg.common.api.vo.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将统一奖励业务异常转换为机器可识别响应。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TsRewardExceptionHandler {

    /** 处理统一奖励业务异常。 */
    @ExceptionHandler(TsRewardBizException.class)
    public Result<Void> handleRewardBizException(
            TsRewardBizException exception) {
        Result<Void> result = Result.error(
                HttpStatus.CONFLICT.value(), exception.getMessage());
        result.setErrorCode(exception.getErrorCode().name());
        result.setErrorCategory("REWARD");
        result.setRetryable(false);
        return result;
    }
}
