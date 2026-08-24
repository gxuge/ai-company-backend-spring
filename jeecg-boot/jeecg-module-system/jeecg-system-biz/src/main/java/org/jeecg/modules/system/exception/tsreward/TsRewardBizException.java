package org.jeecg.modules.system.exception.tsreward;

import lombok.Getter;
import org.jeecg.modules.system.enums.tsreward.TsRewardErrorCode;

/** 携带机器错误码的统一奖励业务异常。 */
@Getter
public class TsRewardBizException extends RuntimeException {

    private final TsRewardErrorCode errorCode;

    /** 创建统一奖励业务异常。 */
    public TsRewardBizException(TsRewardErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
