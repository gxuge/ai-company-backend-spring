package org.jeecg.modules.openapi.service;

import java.io.OutputStream;
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
     * @param speed 语速（可空）
     * @param pitch 音调（可空）
     * @param volume 音量（可空）
     * @return 音频十六进制内容
     */
    String textToSpeech(String text, String voiceId, Double speed, Double pitch, Double volume);

    /**
     * 流式执行文本转语音，并将 MP3 字节写入指定输出流。
     *
     * @param text 文本内容
     * @param voiceId 音色 ID
     * @param speed 语速（可空）
     * @param pitch 音调（可空）
     * @param volume 音量（可空）
     * @param outputStream 下游响应输出流
     */
    void streamTextToSpeech(
            String text,
            String voiceId,
            Double speed,
            Double pitch,
            Double volume,
            OutputStream outputStream);

    /**
     * 文生图。
     *
     * @param prompt 提示词
     * @return 图片地址列表
     */
    List<String> generateImage(String prompt);

    /**
     * 图生图。
     *
     * @param prompt 提示词
     * @param referenceImageUrl 参考图地址
     * @return 图片地址列表
     */
    List<String> generateImage(String prompt, String referenceImageUrl);
}
