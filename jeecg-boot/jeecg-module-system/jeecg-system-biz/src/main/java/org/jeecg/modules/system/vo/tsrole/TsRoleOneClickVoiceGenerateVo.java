package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

@Data
public class TsRoleOneClickVoiceGenerateVo {
    /** 统一声音结构，供前端渲染与落库使用 */
    private VoiceMeta voice;
    /** 选中的音色档案ID（兼容旧字段） */
    private Long voiceProfileId;
    /** 音色名称（兼容旧字段） */
    private String voiceName;
    /** 供应商音色ID（兼容旧字段） */
    private String providerVoiceId;
    /** 推荐理由（兼容旧字段） */
    private String recommendation;
    /** 试听文案（兼容旧字段） */
    private String previewText;
    /** 试听音频URL（兼容旧字段） */
    private String previewAudioUrl;
    /** 语速（0.8~1.2，兼容旧字段） */
    private Double speed;
    /** 音调（-6~6，兼容旧字段） */
    private Double pitch;
    /** 音量（0.8~1.2，兼容旧字段） */
    private Double volume;
    /** 使用的Prompt编码 */
    private String promptCode;
    /** 使用的Prompt版本 */
    private String promptVersion;
    /** 实际渲染后的Prompt */
    private String renderedPrompt;
    /** Redis快照Key */
    private String snapshotKey;
    /** 匹配来源：ai_json/fallback_rule/manual/random_pool（兼容旧字段） */
    private String matchSource;
    /** 追踪ID（兼容旧字段） */
    private String traceId;
    /** 返回结构版本（兼容旧字段） */
    private String schemaVersion;

    @Data
    public static class VoiceMeta {
        /** 音色显示名 */
        private String voiceName;
        /** 音色推荐性别 */
        private String voiceGender;
        /** 音色档案ID */
        private Long voiceProfileId;
        /** 供应商音色ID */
        private String providerVoiceId;
        /** 试听文案 */
        private String previewText;
        /** 试听音频链接 */
        private String previewAudioUrl;
        /** 语速（0.8~1.2） */
        private Double speed;
        /** 音调（-6~6） */
        private Double pitch;
        /** 音量（0.8~1.2） */
        private Double volume;
        /** 推荐理由 */
        private String selectionReason;
        /** 匹配来源：ai_json/fallback_rule/manual/random_pool */
        private String matchSource;
        /** 追踪ID */
        private String traceId;
        /** 返回结构版本 */
        private String schemaVersion;
    }
}
