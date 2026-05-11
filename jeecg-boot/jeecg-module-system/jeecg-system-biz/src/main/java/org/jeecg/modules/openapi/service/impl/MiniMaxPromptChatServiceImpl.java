package org.jeecg.modules.openapi.service.impl;

import jakarta.annotation.Resource;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * MiniMax 文本服务适配器（用于模板渲染链路）。
 */
@Service("miniMaxPromptChatService")
public class MiniMaxPromptChatServiceImpl implements IPromptChatService {
    @Resource
    private IMiniMaxDemoService miniMaxDemoService;

    @Override
    public String provider() {
        return "minimax";
    }

    @Override
    public String chat(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new JeecgBootBizTipException("prompt不能为空");
        }
        MiniMaxChatRequestDto requestDto = new MiniMaxChatRequestDto();
        requestDto.setPrompt(prompt);
        MiniMaxChatResponseVo response = miniMaxDemoService.chat(requestDto);
        String content = response == null ? null : response.getContent();
        if (!StringUtils.hasText(content)) {
            throw new JeecgBootBizTipException("MiniMax chat response is empty");
        }
        return content.trim();
    }
}

