package org.jeecg.common.model;

/**
 * 接口响应日志实体：
 * 用于承载响应阶段需要打印的核心字段。
 */
public class ApiResponseLog {

    /** 接口耗时（毫秒） */
    private final long costMs;
    /** 成功标记 */
    private final boolean success;
    /** 业务返回码 */
    private final String code;
    /** 返回消息 */
    private final String msg;
    /** 返回结果摘要 */
    private final String result;

    public ApiResponseLog(long costMs, boolean success, String code, String msg, String result) {
        this.costMs = costMs;
        this.success = success;
        this.code = code;
        this.msg = msg;
        this.result = result;
    }

    public long getCostMs() {
        return costMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getResult() {
        return result;
    }
}
