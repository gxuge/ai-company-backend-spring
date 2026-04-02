package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

import java.util.Date;

@Data
public class TsChatAiReplyVo {
    /** 会话 ID */
    private Long sessionId;
    /** 用户消息 ID */
    private Long userMessageId;
    /** AI 回复消息 ID */
    private Long assistantMessageId;
    /** 语音附件 ID */
    private Long attachmentId;
    /** 命中的音色档案 ID */
    private Long voiceProfileId;
    /** 实际使用的 MiniMax 音色 ID */
    private String voiceId;
    /** AI 回复文本 */
    private String contentText;
    /** 语音可播放地址 */
    private String audioUrl;
    /** 语音文件大小（字节） */
    private Long audioFileSize;
    /** 语音时长（秒），当前由下游能力决定是否返回 */
    private Integer durationSec;
    /** 语音 MIME 类型 */
    private String mimeType;
    /** AI 回复创建时间 */
    private Date createdAt;
}
