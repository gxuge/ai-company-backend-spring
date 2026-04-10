package org.jeecg.common.model;

/**
 * 接口请求日志实体：
 * 用于承载请求阶段需要打印的核心字段。
 */
public class ApiRequestLog {

    /** 显眼化后的接口路径 */
    private final String endpoint;
    /** HTTP 方法 */
    private final String method;
    /** Controller.方法名 */
    private final String handler;
    /** 序列化后的入参 */
    private final String params;

    public ApiRequestLog(String endpoint, String method, String handler, String params) {
        this.endpoint = endpoint;
        this.method = method;
        this.handler = handler;
        this.params = params;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public String getHandler() {
        return handler;
    }

    public String getParams() {
        return params;
    }
}
