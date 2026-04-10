package org.jeecg.modules.system.vo.tsvoiceprofile;

import lombok.Data;

@Data
public class TsVoiceProfilePreviewVo {
    /** 选中音色档案 ID */
    private Long voiceProfileId;
    /** 音色名称 */
    private String voiceName;
    /** 提供商音色 ID */
    private String providerVoiceId;
    /** 试听文案 */
    private String previewText;
    /** 试听音频 URL */
    private String previewAudioUrl;
}
