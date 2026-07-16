package org.jeecg.modules.system.vo.tsagentchatsession;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Agent 会话消息前端展示对象。
 *
 * @author codex
 * @date 2026/7/15
 */
@Data
public class TsAgentChatMessageVo {

    /**
     * 消息ID。
     */
    private Long id;

    /**
     * 消息角色：user/assistant。
     */
    private String roleType;

    /**
     * 消息正文。
     */
    private String content;

    /**
     * 消息状态：streaming/success/failed。
     */
    private String messageStatus;

    /**
     * 创建时间。
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 当前消息触发的 SubAgent/Tool 事件。
     */
    private List<TsAgentChatMessageEventVo> events = new ArrayList<>();
}
