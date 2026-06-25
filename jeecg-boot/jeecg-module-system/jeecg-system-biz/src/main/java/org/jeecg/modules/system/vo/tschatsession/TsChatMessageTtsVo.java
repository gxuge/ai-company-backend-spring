package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

import java.util.Date;

@Data
public class TsChatMessageTtsVo {

    /** 会话 ID */
    private Long sessionId;
    /** 消息 ID */
    private Long messageId;
    /** 命中的音色档案 ID */
    private Long voiceProfileId;
    /** 实际使用的音色 ID */
    private String voiceId;
    /** 实际用于播报的文本 */
    private String ttsText;
    /** 语音可播放地址 */
    private String audioUrl;
    /** 前端本地语音缓存键 */
    private String audioCacheKey;
    /** 语音文件大小（字节） */
    private Long audioFileSize;
    /** 语音时长（秒） */
    private Integer durationSec;
    /** 语音 MIME 类型 */
    private String mimeType;
    /** 更新时间 */
    private Date createdAt;
}
