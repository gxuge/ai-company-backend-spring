package org.jeecg.modules.system.exception.tsad;

import lombok.Getter;
import org.jeecg.modules.system.enums.tsad.TsAdErrorCode;

/** 广告投放域业务异常。 */
@Getter
public class TsAdBizException extends RuntimeException {
    private final TsAdErrorCode errorCode;

    /** 创建不可重试的广告业务异常。 */
    public TsAdBizException(TsAdErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
