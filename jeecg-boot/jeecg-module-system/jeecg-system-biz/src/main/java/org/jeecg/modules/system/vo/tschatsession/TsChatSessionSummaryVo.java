package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

/**
 * 会话列表展示摘要。
 */
@Data
public class TsChatSessionSummaryVo {
    /** 会话ID */
    private Long sessionId;
    /** 目标角色名称 */
    private String roleName;
    /** 目标角色头像地址 */
    private String roleAvatarUrl;
    /** 最后一条消息文本 */
    private String lastMessageText;
}
