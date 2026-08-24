package org.jeecg.modules.system.exception.tspoints;

import lombok.Getter;
import org.jeecg.modules.system.enums.tspoints.TsPointsErrorCode;

import java.util.Collections;
import java.util.Map;

/** 携带机器错误码的积分业务异常。 */
@Getter
public class TsPointsBizException extends RuntimeException {

    private final TsPointsErrorCode errorCode;
    private final boolean retryable;
    private final Map<String, Object> errorArgs;

    /** 创建不可重试的积分业务异常。 */
    public TsPointsBizException(TsPointsErrorCode errorCode, String message) {
        this(errorCode, message, false, Collections.emptyMap());
    }

    /** 创建带错误参数的积分业务异常。 */
    public TsPointsBizException(
            TsPointsErrorCode errorCode,
            String message,
            boolean retryable,
            Map<String, Object> errorArgs) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.errorArgs = errorArgs == null ? Collections.emptyMap() : errorArgs;
    }
}
