package org.jeecg.modules.airag.safety.moderation;

import org.jeecg.modules.airag.safety.moderation.adapter.LlmJsonModerationResponseAdapter;
import org.jeecg.modules.airag.safety.moderation.adapter.ModerationProviderDecision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ModerationCoreTest {

    @Test
    void shouldAdaptProviderJsonAndApplyScorePolicy() {
        LlmJsonModerationResponseAdapter adapter = new LlmJsonModerationResponseAdapter();
        ModerationProviderDecision decision = adapter.adapt(
                "```json\n{\"safe\":false,\"category\":\"sexual_minor\","
                        + "\"score\":0.91,\"uncertain\":false,\"reason\":\"涉及未成年人风险\"}\n```"
        );

        ModerationResult result = new ModerationRiskPolicy()
                .evaluate(decision, "test-service", false);

        Assertions.assertFalse(result.isSafe());
        Assertions.assertEquals(ModerationCategory.SEXUAL_MINOR, result.getCategory());
        Assertions.assertEquals(0.91D, result.getScore(), 0.0001D);
        Assertions.assertEquals(ModerationAction.BLOCK, result.getAction());
        Assertions.assertEquals("test-service", result.getModerationService());
    }

    @Test
    void shouldRewriteMediumRiskOutputAndReviewAgain() {
        ModerationService moderationService = Mockito.mock(ModerationService.class);
        Mockito.when(moderationService.moderate(Mockito.any()))
                .thenReturn(result(ModerationAction.SAFE_REPLY, 0.6D))
                .thenReturn(result(ModerationAction.ALLOW, 0.1D));
        ModerationGuard guard = new ModerationGuard(moderationService);
        AtomicInteger rewriteCalls = new AtomicInteger();

        String output = guard.reviewOutput(
                "model-1",
                "test",
                "风险输出",
                List.of(),
                "request-1",
                value -> {
                    rewriteCalls.incrementAndGet();
                    return "安全改写";
                }
        );

        Assertions.assertEquals("安全改写", output);
        Assertions.assertEquals(1, rewriteCalls.get());
        Mockito.verify(moderationService, Mockito.times(2)).moderate(Mockito.any());
    }

    @Test
    void shouldDiscardHighRiskOutputWithoutRewrite() {
        ModerationService moderationService = Mockito.mock(ModerationService.class);
        Mockito.when(moderationService.moderate(Mockito.any()))
                .thenReturn(result(ModerationAction.BLOCK, 0.9D));
        ModerationGuard guard = new ModerationGuard(moderationService);
        AtomicInteger rewriteCalls = new AtomicInteger();

        String output = guard.reviewOutput(
                "model-1",
                "test",
                "高风险输出",
                List.of(),
                "request-1",
                value -> {
                    rewriteCalls.incrementAndGet();
                    return "不应执行";
                }
        );

        Assertions.assertEquals(ModerationGuard.SAFE_REPLY, output);
        Assertions.assertEquals(0, rewriteCalls.get());
        Mockito.verify(moderationService).moderate(Mockito.any());
    }

    private ModerationResult result(ModerationAction action, double score) {
        return ModerationResult.builder()
                .safe(action == ModerationAction.ALLOW)
                .category(action == ModerationAction.ALLOW
                        ? ModerationCategory.NONE
                        : ModerationCategory.VIOLENCE)
                .score(score)
                .action(action)
                .reason("test")
                .moderationService("test")
                .build();
    }
}
