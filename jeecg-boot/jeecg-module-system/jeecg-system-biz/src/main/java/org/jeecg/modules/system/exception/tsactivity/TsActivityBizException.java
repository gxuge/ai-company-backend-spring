package org.jeecg.modules.system.exception.tsactivity;

import lombok.Getter;
import org.jeecg.modules.system.enums.tsactivity.TsActivityErrorCode;

import java.util.Collections;
import java.util.Map;

/** 活动域机器可识别业务异常。 */
@Getter
public class TsActivityBizException extends RuntimeException {

    private final TsActivityErrorCode errorCode;
    private final boolean retryable;
    private final Map<String, Object> errorArgs;

    /** 创建不可重试的活动业务异常。 */
    public TsActivityBizException(TsActivityErrorCode errorCode, String message) {
        this(errorCode, message, false, Collections.emptyMap());
    }

    /** 创建带错误参数的活动业务异常。 */
    public TsActivityBizException(
            TsActivityErrorCode errorCode,
            String message,
            boolean retryable,
            Map<String, Object> errorArgs) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.errorArgs = errorArgs == null ? Collections.emptyMap() : errorArgs;
    }
}
