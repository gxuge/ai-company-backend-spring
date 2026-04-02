package org.jeecg.modules.openapi.vo;

import lombok.Data;

/**
 * MiniMax 语音响应 VO。
 */
@Data
public class MiniMaxTtsResponseVo {

    /**
     * 音频十六进制内容。
     */
    private String audioHex;
    private String audioUrl;
}
