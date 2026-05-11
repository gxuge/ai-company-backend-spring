package org.jeecg.modules.openapi.service.impl;

import jakarta.annotation.Resource;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Prompt 文本服务路由：按配置在 DeepSeek、Qwen 与 MiniMax 间切换。
 */
@Primary
@Service
public class PromptChatRoutingServiceImpl implements IPromptChatService {
    @Resource
    private PromptChatConfigBean promptChatConfigBean;
    @Resource(name = "deepSeekPromptChatService")
    private IPromptChatService deepSeekPromptChatService;
    @Resource(name = "qwenPromptChatService")
    private IPromptChatService qwenPromptChatService;
    @Resource(name = "miniMaxPromptChatService")
    private IPromptChatService miniMaxPromptChatService;

    @Override
    public String provider() {
        return resolveDelegate().provider();
    }

    @Override
    public String chat(String prompt) {
        return resolveDelegate().chat(prompt);
    }

    private IPromptChatService resolveDelegate() {
        String provider = promptChatConfigBean.getProvider();
        if (!StringUtils.hasText(provider)) {
            return qwenPromptChatService;
        }
        if ("deepseek".equalsIgnoreCase(provider.trim())) {
            return deepSeekPromptChatService;
        }
        if ("minimax".equalsIgnoreCase(provider.trim())) {
            return miniMaxPromptChatService;
        }
        if ("qwen".equalsIgnoreCase(provider.trim())) {
            return qwenPromptChatService;
        }
        return qwenPromptChatService;
    }
}
