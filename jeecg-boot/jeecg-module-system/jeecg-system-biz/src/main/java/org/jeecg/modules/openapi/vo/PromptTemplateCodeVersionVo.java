package org.jeecg.modules.openapi.vo;

/**
 * Prompt 模板 code/version 解析结果。
 */
public class PromptTemplateCodeVersionVo {
    /**
     * Prompt 模板编码，如 role_core_fill。
     */
    private final String code;
    /**
     * Prompt 模板版本，如 v1。
     */
    private final String version;

    public PromptTemplateCodeVersionVo(String code, String version) {
        this.code = code;
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public String getVersion() {
        return version;
    }
}
