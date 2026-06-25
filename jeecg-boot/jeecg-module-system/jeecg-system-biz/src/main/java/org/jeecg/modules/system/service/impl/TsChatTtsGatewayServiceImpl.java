package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.airag.llm.consts.LLMConsts;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.mapper.AiragModelMapper;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;
import org.jeecg.modules.system.dto.tschatsession.TsChatTtsSynthesizeDto;
import org.jeecg.modules.system.service.ITsChatTtsGatewayService;
import org.jeecg.modules.system.vo.tschatsession.TsChatTtsResultVo;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Slf4j
public class TsChatTtsGatewayServiceImpl implements ITsChatTtsGatewayService {

    private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";

    @Resource
    private PromptChatConfigBean promptChatConfigBean;
    @Resource
    private AiragAppMapper airagAppMapper;
    @Resource
    private AiragModelMapper airagModelMapper;
    @Resource
    private IMiniMaxDemoService miniMaxDemoService;

    @Override
    public TsChatTtsResultVo synthesizeForChat(TsChatTtsSynthesizeDto request) {
        if (request == null || !StringUtils.hasText(request.getText())) {
            throw new JeecgBootBizTipException("语音文本不能为空");
        }

        AiragApp app = resolveApp();
        AiragModel voiceModel = resolveVoiceModel(app);
        String text = request.getText().trim();
        String textHash = DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
        String voiceId = resolveVoiceId(request.getVoiceId(), voiceModel);
        String cacheKey = buildCacheKey(app.getId(), voiceModel.getId(), voiceModel.getProvider(), voiceModel.getModelName(),
                voiceId, textHash, request.getSpeed(), request.getPitch(), request.getVolume());

        String provider = trimToNull(voiceModel.getProvider());
        return switchProviderGenerate(provider, voiceModel, request, voiceId, textHash, cacheKey, app.getId());
    }

    private AiragApp resolveApp() {
        String appId = trimToNull(promptChatConfigBean.getAppId());
        if (!StringUtils.hasText(appId)) {
            throw new JeecgBootBizTipException("未配置 jeecg.airag.prompt-chat.app-id，无法解析聊天语音模型");
        }
        AiragApp app = airagAppMapper.getByIdIgnoreTenant(appId);
        if (app == null) {
            throw new JeecgBootBizTipException("未找到AI应用配置，appId=" + appId);
        }
        return app;
    }

    private AiragModel resolveVoiceModel(AiragApp app) {
        String voiceModelId = trimToNull(app == null ? null : app.getVoiceModelId());
        if (!StringUtils.hasText(voiceModelId)) {
            throw new JeecgBootBizTipException("当前AI应用未配置语音模型，appId=" + (app == null ? null : app.getId()));
        }
        AiragModel model = airagModelMapper.getByIdIgnoreTenant(voiceModelId);
        if (model == null) {
            throw new JeecgBootBizTipException("未找到语音模型配置，voiceModelId=" + voiceModelId);
        }
        if (!LLMConsts.MODEL_TYPE_VOICE.equalsIgnoreCase(trimToNull(model.getModelType()))) {
            throw new JeecgBootBizTipException("当前模型不是语音模型，voiceModelId=" + voiceModelId);
        }
        if (model.getActivateFlag() == null || model.getActivateFlag() != 1) {
            throw new JeecgBootBizTipException("语音模型未激活，请先完成测试激活，voiceModelId=" + voiceModelId);
        }
        return model;
    }

    private String resolveVoiceId(String requestVoiceId, AiragModel voiceModel) {
        String directVoiceId = trimToNull(requestVoiceId);
        if (StringUtils.hasText(directVoiceId)) {
            return directVoiceId;
        }
        JSONObject modelParams = parseModelParams(voiceModel == null ? null : voiceModel.getModelParams());
        String modelVoiceId = trimToNull(modelParams == null ? null : modelParams.getString("voiceId"));
        if (!StringUtils.hasText(modelVoiceId)) {
            modelVoiceId = trimToNull(modelParams == null ? null : modelParams.getString("providerVoiceId"));
        }
        if (!StringUtils.hasText(modelVoiceId)) {
            throw new JeecgBootBizTipException("当前语音模型未配置默认音色，且本次请求未指定 voiceId");
        }
        return modelVoiceId;
    }

    private TsChatTtsResultVo switchProviderGenerate(String provider,
                                                     AiragModel voiceModel,
                                                     TsChatTtsSynthesizeDto request,
                                                     String voiceId,
                                                     String textHash,
                                                     String cacheKey,
                                                     String appId) {
        String normalizedProvider = StringUtils.hasText(provider) ? provider.trim().toUpperCase(Locale.ROOT) : "";
        if ("MINIMAX".equals(normalizedProvider)) {
            return generateByMiniMax(voiceModel, request, voiceId, textHash, cacheKey, appId);
        }
        throw new JeecgBootBizTipException("当前语音模型暂不支持 provider=" + provider);
    }

    private TsChatTtsResultVo generateByMiniMax(AiragModel voiceModel,
                                                TsChatTtsSynthesizeDto request,
                                                String voiceId,
                                                String textHash,
                                                String cacheKey,
                                                String appId) {
        MiniMaxTtsRequestDto ttsRequest = new MiniMaxTtsRequestDto();
        ttsRequest.setText(request.getText());
        ttsRequest.setVoiceId(voiceId);
        ttsRequest.setSpeed(request.getSpeed());
        ttsRequest.setPitch(request.getPitch());
        ttsRequest.setVolume(request.getVolume());
        MiniMaxTtsResponseVo response = miniMaxDemoService.tts(ttsRequest);
        String audioUrl = response == null ? null : trimToNull(response.getAudioUrl());
        if (!StringUtils.hasText(audioUrl)) {
            throw new JeecgBootBizTipException("语音生成成功但未返回可播放地址，请检查当前语音模型上传配置");
        }

        TsChatTtsResultVo result = new TsChatTtsResultVo();
        result.setCacheKey(cacheKey);
        result.setAppId(appId);
        result.setVoiceModelId(voiceModel.getId());
        result.setProvider(trimToNull(voiceModel.getProvider()));
        result.setModelName(trimToNull(voiceModel.getModelName()));
        result.setVoiceId(voiceId);
        result.setTextHash(textHash);
        result.setCacheHit(Boolean.FALSE);
        result.setAudioUrl(audioUrl);
        result.setMimeType(MIME_TYPE_AUDIO_MPEG);
        result.setFileSize(estimateAudioSize(response == null ? null : response.getAudioHex()));
        result.setDurationSec(null);
        return result;
    }

    private String buildCacheKey(String appId,
                                 String voiceModelId,
                                 String provider,
                                 String modelName,
                                 String voiceId,
                                 String textHash,
                                 Double speed,
                                 Double pitch,
                                 Double volume) {
        String raw = String.join("|",
                defaultString(appId),
                defaultString(voiceModelId),
                defaultString(provider),
                defaultString(modelName),
                defaultString(voiceId),
                defaultString(textHash),
                normalizeNumber(speed),
                normalizeNumber(pitch),
                normalizeNumber(volume));
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject parseModelParams(String modelParams) {
        if (!StringUtils.hasText(modelParams)) {
            return null;
        }
        try {
            return JSONObject.parseObject(modelParams);
        } catch (Exception ex) {
            log.warn("parse voice modelParams failed: {}", ex.getMessage());
            return null;
        }
    }

    private Long estimateAudioSize(String audioHex) {
        if (!StringUtils.hasText(audioHex)) {
            return null;
        }
        String cleanHex = audioHex.trim();
        if (cleanHex.startsWith("0x") || cleanHex.startsWith("0X")) {
            cleanHex = cleanHex.substring(2);
        }
        cleanHex = cleanHex.replaceAll("\\s+", "");
        if (cleanHex.isEmpty()) {
            return null;
        }
        if ((cleanHex.length() & 1) == 1) {
            cleanHex = "0" + cleanHex;
        }
        return cleanHex.length() / 2L;
    }

    private String normalizeNumber(Double value) {
        return value == null ? "" : String.format(Locale.ROOT, "%.2f", value);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
