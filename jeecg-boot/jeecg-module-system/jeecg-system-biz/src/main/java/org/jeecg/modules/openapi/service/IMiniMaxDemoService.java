package org.jeecg.modules.openapi.service;

import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;

/**
 * MiniMax 演示接口业务服务。
 */
public interface IMiniMaxDemoService {

    /**
     * 文本对话。
     *
     * @param requestDto 对话请求
     * @return 对话响应
     */
    MiniMaxChatResponseVo chat(MiniMaxChatRequestDto requestDto);

    /**
     * 文本转语音。
     *
     * @param requestDto 语音请求
     * @return 语音响应
     */
    MiniMaxTtsResponseVo tts(MiniMaxTtsRequestDto requestDto);

    /**
     * 文生图。
     *
     * @param requestDto 文生图请求
     * @return 文生图响应
     */
    MiniMaxImageResponseVo image(MiniMaxImageRequestDto requestDto);
}
