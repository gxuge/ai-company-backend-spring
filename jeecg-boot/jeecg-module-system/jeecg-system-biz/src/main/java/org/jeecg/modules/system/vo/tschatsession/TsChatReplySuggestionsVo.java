package org.jeecg.modules.system.vo.tschatsession;

import lombok.Data;

import java.util.List;

@Data
public class TsChatReplySuggestionsVo {
    /** 会话 ID */
    private Long sessionId;
    /** 候选回复（固定 3 条） */
    private List<String> suggestions;
    /** 提示词编码 */
    private String promptCode;
    /** 提示词版本 */
    private String promptVersion;
    /** 渲染后的提示词 */
    private String renderedPrompt;
    /** 快照键 */
    private String snapshotKey;
}
