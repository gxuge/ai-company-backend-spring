package org.jeecg.modules.openapi.service.impl;

import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.airag.agent.safety.GlobalSafetySkillPromptProvider;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.mapper.AiragModelMapper;
import org.jeecg.modules.airag.safety.moderation.ModerationAction;
import org.jeecg.modules.airag.safety.moderation.ModerationCategory;
import org.jeecg.modules.airag.safety.moderation.ModerationGuard;
import org.jeecg.modules.airag.safety.moderation.ModerationResult;
import org.jeecg.modules.openapi.config.MiniMaxDemoConfigBean;
import org.jeecg.modules.openapi.config.MiniMaxDemoGuardConfigBean;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxMediaService;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class MiniMaxSafetyPromptIntegrationTest {

    @Test
    void shouldPrependSafetyPromptForPromptChat() {
        GlobalSafetySkillPromptProvider provider = Mockito.mock(GlobalSafetySkillPromptProvider.class);
        Mockito.when(provider.prependToSystemPrompt("业务规则"))
                .thenReturn("安全规则\n\n业务规则");
        MiniMaxPromptChatServiceImpl service = new MiniMaxPromptChatServiceImpl();
        ReflectionTestUtils.setField(service, "globalSafetySkillPromptProvider", provider);

        String systemPrompt = service.buildSafeSystemPrompt("业务规则");

        Assertions.assertEquals("安全规则\n\n业务规则", systemPrompt);
    }

    @Test
    void shouldUseSharedProviderForPlainChatAndImagePrompt() {
        ChatClient.Builder chatClientBuilder = Mockito.mock(ChatClient.Builder.class);
        Mockito.when(chatClientBuilder.build()).thenReturn(Mockito.mock(ChatClient.class));
        GlobalSafetySkillPromptProvider provider = Mockito.mock(GlobalSafetySkillPromptProvider.class);
        Mockito.when(provider.requiredSafetyPrompt()).thenReturn("安全规则");
        Mockito.when(provider.buildImageGenerationPrompt("一座雪山"))
                .thenReturn("安全规则\n\n原始图片提示词：\n一座雪山");
        MiniMaxDemoServiceImpl service = new MiniMaxDemoServiceImpl(
                chatClientBuilder,
                Mockito.mock(IMiniMaxMediaService.class),
                Mockito.mock(MiniMaxDemoGuardConfigBean.class),
                Mockito.mock(MiniMaxDemoConfigBean.class),
                Mockito.mock(JeecgBaseConfig.class),
                Mockito.mock(Environment.class),
                provider,
                Mockito.mock(ModerationGuard.class),
                Mockito.mock(PromptChatConfigBean.class),
                Mockito.mock(AiragAppMapper.class)
        );

        Assertions.assertEquals("安全规则", service.buildSafetySystemPrompt());
        Assertions.assertEquals(
                "安全规则\n\n原始图片提示词：\n一座雪山",
                service.buildSafeImagePrompt("一座雪山")
        );
    }

    @Test
    void shouldNotCallPromptModelWhenInputIsBlocked() {
        MiniMaxPromptChatServiceImpl service = new MiniMaxPromptChatServiceImpl();
        PromptChatConfigBean config = new PromptChatConfigBean();
        config.setModelId("model-1");
        AiragModel model = new AiragModel();
        model.setId("model-1");
        model.setModelType(LLMConsts.MODEL_TYPE_LLM);
        model.setActivateFlag(1);
        model.setProvider("MINIMAX");
        model.setModelName("test-model");
        AiragModelMapper modelMapper = Mockito.mock(AiragModelMapper.class);
        Mockito.when(modelMapper.getByIdIgnoreTenant("model-1")).thenReturn(model);
        ModerationGuard guard = Mockito.mock(ModerationGuard.class);
        ModerationResult blocked = blockedResult();
        Mockito.when(guard.reviewInput(
                Mockito.eq("model-1"),
                Mockito.eq("prompt_chat"),
                Mockito.eq("风险请求"),
                Mockito.anyList(),
                Mockito.isNull()
        )).thenReturn(blocked);
        Mockito.when(guard.isAllowed(blocked)).thenReturn(false);
        Mockito.when(guard.safeReply()).thenReturn(ModerationGuard.SAFE_REPLY);
        IAIChatHandler chatHandler = Mockito.mock(IAIChatHandler.class);
        ReflectionTestUtils.setField(service, "promptChatConfigBean", config);
        ReflectionTestUtils.setField(service, "airagModelMapper", modelMapper);
        ReflectionTestUtils.setField(service, "moderationGuard", guard);
        ReflectionTestUtils.setField(service, "aiChatHandler", chatHandler);

        String response = service.chat("风险请求");

        Assertions.assertEquals(ModerationGuard.SAFE_REPLY, response);
        Mockito.verifyNoInteractions(chatHandler);
    }

    @Test
    void shouldNotCallImageProviderWhenPromptIsBlocked() {
        ChatClient.Builder chatClientBuilder = Mockito.mock(ChatClient.Builder.class);
        Mockito.when(chatClientBuilder.build()).thenReturn(Mockito.mock(ChatClient.class));
        IMiniMaxMediaService mediaService = Mockito.mock(IMiniMaxMediaService.class);
        MiniMaxDemoGuardConfigBean guardConfig = Mockito.mock(MiniMaxDemoGuardConfigBean.class);
        Mockito.when(guardConfig.getMaxImagePromptChars()).thenReturn(1000);
        PromptChatConfigBean promptChatConfig = new PromptChatConfigBean();
        promptChatConfig.setModelId("model-1");
        ModerationGuard moderationGuard = Mockito.mock(ModerationGuard.class);
        ModerationResult blocked = blockedResult();
        Mockito.when(moderationGuard.reviewImagePrompt(
                "model-1", "minimax_demo_image", "风险图片", null
        )).thenReturn(blocked);
        Mockito.when(moderationGuard.isAllowed(blocked)).thenReturn(false);
        Mockito.when(moderationGuard.safeReply()).thenReturn(ModerationGuard.SAFE_REPLY);
        MiniMaxDemoServiceImpl service = new MiniMaxDemoServiceImpl(
                chatClientBuilder,
                mediaService,
                guardConfig,
                Mockito.mock(MiniMaxDemoConfigBean.class),
                Mockito.mock(JeecgBaseConfig.class),
                Mockito.mock(Environment.class),
                Mockito.mock(GlobalSafetySkillPromptProvider.class),
                moderationGuard,
                promptChatConfig,
                Mockito.mock(AiragAppMapper.class)
        );
        MiniMaxImageRequestDto request = new MiniMaxImageRequestDto();
        request.setPrompt("风险图片");

        JeecgBootBizTipException exception = Assertions.assertThrows(
                JeecgBootBizTipException.class,
                () -> service.image(request)
        );

        Assertions.assertEquals(ModerationGuard.SAFE_REPLY, exception.getMessage());
        Mockito.verifyNoInteractions(mediaService);
    }

    private ModerationResult blockedResult() {
        return ModerationResult.builder()
                .safe(false)
                .category(ModerationCategory.ILLEGAL)
                .score(0.9D)
                .action(ModerationAction.BLOCK)
                .reason("test")
                .moderationService("test")
                .build();
    }
}
