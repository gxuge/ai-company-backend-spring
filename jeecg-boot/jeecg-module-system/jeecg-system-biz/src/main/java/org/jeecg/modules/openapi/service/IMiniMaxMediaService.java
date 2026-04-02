package org.jeecg.modules.openapi.service;

import java.util.List;

/**
 * MiniMax 多模态调用服务接口。
 */
public interface IMiniMaxMediaService {

    /**
     * 文本转语音。
     *
     * @param text 文本内容
     * @param voiceId 音色ID
     * @return 音频十六进制内容
     */
    String textToSpeech(String text, String voiceId);

    /**
     * 文生图。
     *
     * @param prompt 提示词
     * @return 图片地址列表
     */
    List<String> generateImage(String prompt);
}
