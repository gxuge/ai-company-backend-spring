package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

@Data
public class TsRoleOneClickVoiceGenerateVo {
    /** 选中的音色档案ID */
    private Long voiceProfileId;
    /** 音色名称 */
    private String voiceName;
    /** 供应商音色ID */
    private String providerVoiceId;
    /** 推荐理由 */
    private String recommendation;
    /** 试听文案 */
    private String previewText;
    /** 试听音频URL */
    private String previewAudioUrl;
    /** 使用的Prompt编码 */
    private String promptCode;
    /** 使用的Prompt版本 */
    private String promptVersion;
    /** 实际渲染后的Prompt */
    private String renderedPrompt;
    /** Redis快照Key */
    private String snapshotKey;
}
