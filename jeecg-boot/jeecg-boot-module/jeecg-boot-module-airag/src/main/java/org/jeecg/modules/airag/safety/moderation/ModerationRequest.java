package org.jeecg.modules.airag.safety.moderation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一文本审核请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRequest {
    /**
     * 当前待审核文本。
     */
    private String content;
    /**
     * 最近几轮对话上下文，仅在意图不明确时使用。
     */
    @Builder.Default
    private List<ModerationContextMessage> recentContext = new ArrayList<>();
    /**
     * 审核阶段。
     */
    private ModerationStage stage;
    /**
     * 当前业务场景。
     */
    private String scene;
    /**
     * 用于线上审核的 AIRAG 模型 ID。
     */
    private String modelId;
    /**
     * 业务请求或运行标识。
     */
    private String requestId;
}
