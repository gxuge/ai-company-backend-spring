package org.jeecg.modules.system.dto.tschatsession;

import lombok.Data;

@Data
public class TsChatTtsSynthesizeDto {

    /**
     * 待合成文本。
     */
    private String text;

    /**
     * 指定音色 ID，可为空；为空时允许走语音模型默认音色。
     */
    private String voiceId;

    /**
     * 命中的音色档案 ID，可为空。
     */
    private Long voiceProfileId;

    /**
     * 语速。
     */
    private Double speed;

    /**
     * 音调。
     */
    private Double pitch;

    /**
     * 音量。
     */
    private Double volume;
}
