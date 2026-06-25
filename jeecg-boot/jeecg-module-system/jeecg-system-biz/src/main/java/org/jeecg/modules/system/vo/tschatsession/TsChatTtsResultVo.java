package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

@Data
public class TsChatTtsResultVo {

    /**
     * 前端本地缓存键。
     */
    private String cacheKey;

    /**
     * 实际使用的应用 ID。
     */
    private String appId;

    /**
     * 实际命中的语音模型 ID。
     */
    private String voiceModelId;

    /**
     * 供应商。
     */
    private String provider;

    /**
     * 模型名称。
     */
    private String modelName;

    /**
     * 实际使用的音色 ID。
     */
    private String voiceId;

    /**
     * 文本哈希。
     */
    private String textHash;

    /**
     * 当前是否命中服务端缓存。
     * 轻量版链路固定为 false，仅保留字段兼容历史返回结构。
     */
    private Boolean cacheHit;

    /**
     * 可播放地址。
     */
    private String audioUrl;

    /**
     * MIME 类型。
     */
    private String mimeType;

    /**
     * 文件大小。
     */
    private Long fileSize;

    /**
     * 时长，当前可为空。
     */
    private Integer durationSec;
}
