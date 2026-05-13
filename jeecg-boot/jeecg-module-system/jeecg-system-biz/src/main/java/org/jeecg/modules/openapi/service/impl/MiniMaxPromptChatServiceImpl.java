package org.jeecg.modules.openapi.service.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.mapper.AiragModelMapper;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Unified prompt chat service.
 *
 * Keeps the historical bean name while routing by AIRAG DB model config.
 */
@Service("miniMaxPromptChatService")
public class MiniMaxPromptChatServiceImpl implements IPromptChatService {

    @Resource
    private PromptChatConfigBean promptChatConfigBean;
    @Resource
    private AiragAppMapper airagAppMapper;
    @Resource
    private AiragModelMapper airagModelMapper;
    @Resource
    private IAIChatHandler aiChatHandler;

    @Override
    public String provider() {
        AiragModel model = resolvePromptModel();
        String provider = model.getProvider();
        return StringUtils.hasText(provider) ? provider.trim().toLowerCase(Locale.ROOT) : "unknown";
    }

    @Override
    public String chat(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new JeecgBootBizTipException("Prompt must not be blank");
        }
        AiragModel model = resolvePromptModel();
        List<ChatMessage> messages = List.of(new UserMessage(prompt));
        String content = aiChatHandler.completions(model.getId(), messages);
        if (!StringUtils.hasText(content)) {
            throw new JeecgBootBizTipException("Prompt chat response is empty");
        }
        return content.trim();
    }

    private AiragModel resolvePromptModel() {
        String modelId = trimToNull(promptChatConfigBean.getModelId());
        if (!StringUtils.hasText(modelId)) {
            String appId = trimToNull(promptChatConfigBean.getAppId());
            if (!StringUtils.hasText(appId)) {
                throw new JeecgBootBizTipException("Please configure jeecg.airag.prompt-chat.app-id or model-id first");
            }
            AiragApp app = airagAppMapper.getByIdIgnoreTenant(appId);
            if (app == null) {
                throw new JeecgBootBizTipException("No AIRAG app found, appId=" + appId);
            }
            modelId = trimToNull(app.getModelId());
            if (!StringUtils.hasText(modelId)) {
                throw new JeecgBootBizTipException("App has no text model configured, appId=" + appId);
            }
        }

        AiragModel model = airagModelMapper.getByIdIgnoreTenant(modelId);
        if (model == null) {
            throw new JeecgBootBizTipException("No AIRAG model found, modelId=" + modelId);
        }
        if (!LLMConsts.MODEL_TYPE_LLM.equalsIgnoreCase(trimToNull(model.getModelType()))) {
            throw new JeecgBootBizTipException("Prompt chat only supports LLM model, current type=" + model.getModelType());
        }
        if (model.getActivateFlag() == null || model.getActivateFlag() != 1) {
            throw new JeecgBootBizTipException("Model is not activated. Please test/activate it first, modelId=" + modelId);
        }
        return model;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
