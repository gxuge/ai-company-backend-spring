package org.jeecg.modules.airag.safety.moderation;

import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.safety.moderation.adapter.ModerationProviderDecision;
import org.jeecg.modules.airag.safety.moderation.adapter.ModerationProviderResponseAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class LlmModerationServiceTest {

    @Test
    void shouldReviewWithRecentContextWhenIntentIsUncertain() {
        IAIChatHandler chatHandler = Mockito.mock(IAIChatHandler.class);
        Mockito.when(chatHandler.completions(Mockito.anyString(), Mockito.anyList(), Mockito.any()))
                .thenReturn("first")
                .thenReturn("second");
        ModerationProviderResponseAdapter adapter = Mockito.mock(ModerationProviderResponseAdapter.class);
        Mockito.when(adapter.adapt("first")).thenReturn(decision(true, true, 0.3D));
        Mockito.when(adapter.adapt("second")).thenReturn(decision(true, false, 0.1D));
        ModerationRiskPolicy policy = new ModerationRiskPolicy();
        ModerationAuditLogger logger = Mockito.mock(ModerationAuditLogger.class);
        LlmModerationService service = new LlmModerationService(chatHandler, adapter, policy, logger);
        ModerationRequest request = ModerationRequest.builder()
                .modelId("model-1")
                .stage(ModerationStage.INPUT)
                .scene("agent")
                .content("继续刚才那个计划")
                .recentContext(List.of(
                        new ModerationContextMessage("user", "前文"),
                        new ModerationContextMessage("assistant", "回复")
                ))
                .build();

        ModerationResult result = service.moderate(request);

        Assertions.assertEquals(ModerationAction.ALLOW, result.getAction());
        Assertions.assertTrue(result.isContextReviewed());
        Mockito.verify(chatHandler, Mockito.times(2))
                .completions(Mockito.eq("model-1"), Mockito.anyList(), Mockito.any());
        Mockito.verify(logger).log(request, result);
    }

    @Test
    void shouldFailClosedWhenModerationModelIsMissing() {
        IAIChatHandler chatHandler = Mockito.mock(IAIChatHandler.class);
        ModerationAuditLogger logger = Mockito.mock(ModerationAuditLogger.class);
        LlmModerationService service = new LlmModerationService(
                chatHandler,
                Mockito.mock(ModerationProviderResponseAdapter.class),
                new ModerationRiskPolicy(),
                logger
        );
        ModerationRequest request = ModerationRequest.builder()
                .stage(ModerationStage.INPUT)
                .content("普通文本")
                .build();

        ModerationResult result = service.moderate(request);

        Assertions.assertEquals(ModerationAction.BLOCK, result.getAction());
        Assertions.assertEquals(ModerationCategory.UNKNOWN, result.getCategory());
        Mockito.verifyNoInteractions(chatHandler);
        Mockito.verify(logger).log(request, result);
    }

    private ModerationProviderDecision decision(boolean safe, boolean uncertain, double score) {
        return ModerationProviderDecision.builder()
                .safe(safe)
                .category(ModerationCategory.NONE)
                .score(score)
                .uncertain(uncertain)
                .reason("test")
                .build();
    }
}
