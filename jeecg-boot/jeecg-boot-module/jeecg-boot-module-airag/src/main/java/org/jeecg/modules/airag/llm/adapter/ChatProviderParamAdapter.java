package org.jeecg.modules.airag.llm.adapter;

import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.llm.entity.AiragModel;

/**
 * Provider 参数适配器。
 */
public interface ChatProviderParamAdapter {

    /**
     * 是否支持当前 provider/model 场景。
     */
    boolean supports(AiragModel model, AIChatParams params);

    /**
     * 返回能力声明。
     */
    ChatProviderCapability capability(AiragModel model, AIChatParams params);

    /**
     * provider 自定义参数映射。
     */
    void adapt(ChatParamAdaptContext context);
}

