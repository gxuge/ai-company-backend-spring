package org.jeecg.modules.airag.safety.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 聊天主流程使用的统一审核编排组件。
 */
@Component
@RequiredArgsConstructor
public class ModerationGuard {
    /**
     * 固定安全回复，不暴露供应商审核细节。
     */
    public static final String SAFE_REPLY =
            "抱歉，这部分内容无法继续处理。你可以换一种安全、非伤害性的方式描述需求，我会尽力帮助你。";

    private final ModerationService moderationService;

    /**
     * 审核主模型输入。
     */
    public ModerationResult reviewInput(String modelId,
                                        String scene,
                                        String content,
                                        List<ModerationContextMessage> recentContext,
                                        String requestId) {
        return this.moderationService.moderate(buildRequest(
                modelId, scene, content, recentContext, requestId, ModerationStage.INPUT
        ));
    }

    /**
     * 审核图片文本 Prompt。
     */
    public ModerationResult reviewImagePrompt(String modelId,
                                              String scene,
                                              String content,
                                              String requestId) {
        return this.moderationService.moderate(buildRequest(
                modelId, scene, content, List.of(), requestId, ModerationStage.IMAGE_PROMPT
        ));
    }

    /**
     * 审核模型输出；中风险安全重写一次后再次审核。
     *
     * @param rewriter 安全重写函数
     * @return 可返回用户的最终文本
     */
    public String reviewOutput(String modelId,
                               String scene,
                               String output,
                               List<ModerationContextMessage> recentContext,
                               String requestId,
                               Function<String, String> rewriter) {
        ModerationResult result = this.moderationService.moderate(buildRequest(
                modelId, scene, output, recentContext, requestId, ModerationStage.OUTPUT
        ));
        if (result.getAction() == ModerationAction.ALLOW) {
            return output;
        }
        if (result.getAction() == ModerationAction.BLOCK || rewriter == null) {
            return SAFE_REPLY;
        }
        try {
            String rewritten = rewriter.apply(output);
            ModerationResult rewrittenResult = this.moderationService.moderate(buildRequest(
                    modelId, scene, rewritten, recentContext, requestId, ModerationStage.OUTPUT_REWRITE
            ));
            return rewrittenResult.getAction() == ModerationAction.ALLOW ? rewritten : SAFE_REPLY;
        } catch (Exception ignored) {
            return SAFE_REPLY;
        }
    }

    /**
     * 判断输入是否允许进入主模型。
     */
    public boolean isAllowed(ModerationResult result) {
        return result != null && result.getAction() == ModerationAction.ALLOW;
    }

    /**
     * 返回固定安全回复。
     */
    public String safeReply() {
        return SAFE_REPLY;
    }

    private ModerationRequest buildRequest(String modelId,
                                           String scene,
                                           String content,
                                           List<ModerationContextMessage> recentContext,
                                           String requestId,
                                           ModerationStage stage) {
        return ModerationRequest.builder()
                .modelId(modelId)
                .scene(scene)
                .content(content)
                .recentContext(recentContext == null ? List.of() : recentContext)
                .requestId(requestId)
                .stage(stage)
                .build();
    }
}
