package org.jeecg.modules.openapi.service.impl;

import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.CloudflareR2Util;
import org.jeecg.common.util.FileDownloadUtils;
import org.jeecg.common.util.MinioUtil;
import org.jeecg.common.util.oss.OssBootUtil;
import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.config.vo.Path;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.airag.agent.safety.GlobalSafetySkillPromptProvider;
import org.jeecg.modules.airag.safety.moderation.ModerationGuard;
import org.jeecg.modules.airag.safety.moderation.ModerationResult;
import org.jeecg.modules.openapi.config.MiniMaxDemoConfigBean;
import org.jeecg.modules.openapi.config.MiniMaxDemoGuardConfigBean;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.IMiniMaxMediaService;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * MiniMax 演示服务实现。
 */
@Service
@Slf4j
public class MiniMaxDemoServiceImpl implements IMiniMaxDemoService {

    private final ChatClient chatClient;
    private final IMiniMaxMediaService miniMaxMediaService;
    private final MiniMaxDemoGuardConfigBean guardConfig;
    private final MiniMaxDemoConfigBean miniMaxDemoConfig;
    private final JeecgBaseConfig jeecgBaseConfig;
    private final Environment environment;
    private final GlobalSafetySkillPromptProvider globalSafetySkillPromptProvider;
    private final ModerationGuard moderationGuard;
    private final PromptChatConfigBean promptChatConfigBean;
    private final AiragAppMapper airagAppMapper;

    public MiniMaxDemoServiceImpl(ChatClient.Builder chatClientBuilder,
                                  IMiniMaxMediaService miniMaxMediaService,
                                  MiniMaxDemoGuardConfigBean guardConfig,
                                  MiniMaxDemoConfigBean miniMaxDemoConfig,
                                  JeecgBaseConfig jeecgBaseConfig,
                                  Environment environment,
                                  GlobalSafetySkillPromptProvider globalSafetySkillPromptProvider,
                                  ModerationGuard moderationGuard,
                                  PromptChatConfigBean promptChatConfigBean,
                                  AiragAppMapper airagAppMapper) {
        this.chatClient = chatClientBuilder.build();
        this.miniMaxMediaService = miniMaxMediaService;
        this.guardConfig = guardConfig;
        this.miniMaxDemoConfig = miniMaxDemoConfig;
        this.jeecgBaseConfig = jeecgBaseConfig;
        this.environment = environment;
        this.globalSafetySkillPromptProvider = globalSafetySkillPromptProvider;
        this.moderationGuard = moderationGuard;
        this.promptChatConfigBean = promptChatConfigBean;
        this.airagAppMapper = airagAppMapper;
    }

    /**
     * MiniMax 文本对话。
     *
     * @param requestDto 对话请求
     * @return 对话结果
     */
    @Override
    public MiniMaxChatResponseVo chat(MiniMaxChatRequestDto requestDto) {
        if (!StringUtils.hasText(requestDto.getPrompt())) {
            throw new JeecgBootBizTipException("prompt不能为空");
        }
        int promptLength = countCodePoints(requestDto.getPrompt());
        int maxChatChars = guardConfig.getMaxChatChars();
        if (promptLength > maxChatChars) {
            throw new JeecgBootBizTipException("prompt长度超过限制，当前长度=" + promptLength + "，上限=" + maxChatChars);
        }
        String moderationModelId = resolveModerationModelId();
        ModerationResult inputModeration = this.moderationGuard.reviewInput(
                moderationModelId, "minimax_demo_chat", requestDto.getPrompt(), List.of(), null
        );
        if (!this.moderationGuard.isAllowed(inputModeration)) {
            return buildModerationChatResponse();
        }
        long startedAt = System.currentTimeMillis();
        ChatResponse chatResponse = invokeChatWithRetry(
                buildSafetySystemPrompt(),
                requestDto.getPrompt()
        );
        String content = chatResponse == null
                || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null
                ? null
                : chatResponse.getResult().getOutput().getText();
        content = this.moderationGuard.reviewOutput(
                moderationModelId,
                "minimax_demo_chat",
                content,
                List.of(),
                null,
                this::rewriteUnsafeChatOutput
        );
        Usage usage = chatResponse == null || chatResponse.getMetadata() == null
                ? null
                : chatResponse.getMetadata().getUsage();
        MiniMaxChatResponseVo responseVo = new MiniMaxChatResponseVo();
        responseVo.setContent(content);
        responseVo.setProvider("MINIMAX");
        responseVo.setModelName(chatResponse == null || chatResponse.getMetadata() == null
                ? null
                : chatResponse.getMetadata().getModel());
        responseVo.setInputTokens(usage == null ? null : usage.getPromptTokens());
        responseVo.setOutputTokens(usage == null ? null : usage.getCompletionTokens());
        responseVo.setTotalTokens(usage == null ? null : usage.getTotalTokens());
        responseVo.setUsageRawJson(usage == null ? null : com.alibaba.fastjson.JSONObject.toJSONString(usage.getNativeUsage()));
        responseVo.setDurationMs(Math.max(0L, System.currentTimeMillis() - startedAt));
        return responseVo;
    }

    /**
     * MiniMax 文本转语音。
     *
     * @param requestDto 语音请求
     * @return 语音结果
     */
    @Override
    public MiniMaxTtsResponseVo tts(MiniMaxTtsRequestDto requestDto) {
        requestDto.normalize();
        if (!StringUtils.hasText(requestDto.getText())) {
            throw new JeecgBootBizTipException("text不能为空");
        }
        if (!StringUtils.hasText(requestDto.getVoiceId())) {
            throw new JeecgBootBizTipException("voiceId不能为空");
        }
        if (requestDto.getText().length() > guardConfig.getMaxTtsChars()) {
            throw new JeecgBootBizTipException("text长度超过限制");
        }
        String audioHex = miniMaxMediaService.textToSpeech(
                requestDto.getText(),
                requestDto.getVoiceId(),
                requestDto.getSpeed(),
                requestDto.getPitch(),
                requestDto.getVolume()
        );
        MiniMaxTtsResponseVo responseVo = new MiniMaxTtsResponseVo();
        responseVo.setAudioHex(audioHex);
        if (miniMaxDemoConfig.isUploadGeneratedMedia()) {
            responseVo.setAudioUrl(uploadAudioHex(audioHex));
        }
        return responseVo;
    }

    /**
     * MiniMax 文生图。
     *
     * @param requestDto 文生图请求
     * @return 绘图结果
     */
    @Override
    public MiniMaxImageResponseVo image(MiniMaxImageRequestDto requestDto) {
        if (requestDto == null) {
            throw new JeecgBootBizTipException("requestDto不能为空");
        }
        requestDto.normalize();
        if (!StringUtils.hasText(requestDto.getPrompt())) {
            throw new JeecgBootBizTipException("prompt不能为空");
        }
        int promptLength = countCodePoints(requestDto.getPrompt());
        int maxImagePromptChars = guardConfig.getMaxImagePromptChars();
        if (promptLength > maxImagePromptChars) {
            throw new JeecgBootBizTipException("prompt长度超过限制，当前长度=" + promptLength + "，上限=" + maxImagePromptChars);
        }
        ModerationResult promptModeration = this.moderationGuard.reviewImagePrompt(
                resolveModerationModelId(),
                "minimax_demo_image",
                requestDto.getPrompt(),
                null
        );
        if (!this.moderationGuard.isAllowed(promptModeration)) {
            throw new JeecgBootBizTipException(this.moderationGuard.safeReply());
        }
        String safeImagePrompt = buildSafeImagePrompt(requestDto.getPrompt());
        List<String> imageUrls = StringUtils.hasText(requestDto.getReferenceImageUrl())
                ? miniMaxMediaService.generateImage(safeImagePrompt, requestDto.getReferenceImageUrl())
                : miniMaxMediaService.generateImage(safeImagePrompt);
        MiniMaxImageResponseVo responseVo = new MiniMaxImageResponseVo();
        responseVo.setOriginalImageUrls(imageUrls);
        if (Boolean.TRUE.equals(requestDto.getUploadGeneratedMedia())) {
            responseVo.setImageUrls(uploadGeneratedImages(imageUrls));
        } else {
            responseVo.setImageUrls(imageUrls);
        }
        return responseVo;
    }

    @Override
    public String persistGeneratedImage(String sourceImageUrl) {
        if (!StringUtils.hasText(sourceImageUrl)) {
            throw new JeecgBootBizTipException("sourceImageUrl不能为空");
        }
        try {
            byte[] imageBytes = downloadBytes(sourceImageUrl.trim());
            String extension = guessImageExtension(sourceImageUrl);
            return uploadBinary(imageBytes, miniMaxDemoConfig.getImageUploadBizPath(), extension);
        } catch (JeecgBootBizTipException e) {
            throw e;
        } catch (Exception e) {
            throw new JeecgBootBizTipException("保存生成图片失败: " + e.getMessage());
        }
    }

    /**
     * 调用 Spring AI ChatClient，并执行重试。
     *
     * @param systemPrompt 系统安全提示词
     * @param prompt 用户输入提示词
     * @return 模型输出文本
     */
    private ChatResponse invokeChatWithRetry(String systemPrompt, String prompt) {
        int maxAttempts = Math.max(miniMaxDemoConfig.getRetryMaxAttempts(), 1);
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(prompt)
                        .call()
                        .chatResponse();
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt >= maxAttempts) {
                    break;
                }
                sleepBeforeRetry();
            }
        }
        String message = lastException == null ? "unknown error" : lastException.getMessage();
        throw new JeecgBootBizTipException("MiniMax chat request failed: " + message);
    }

    /**
     * 读取普通聊天使用的全局安全 System Prompt。
     *
     * @return 安全 System Prompt
     */
    String buildSafetySystemPrompt() {
        return this.globalSafetySkillPromptProvider.requiredSafetyPrompt();
    }

    /**
     * 构建发送给图片供应商的最终安全 Prompt。
     *
     * @param originalPrompt 原始图片提示词
     * @return 最终图片 Prompt
     */
    String buildSafeImagePrompt(String originalPrompt) {
        return this.globalSafetySkillPromptProvider.buildImageGenerationPrompt(originalPrompt);
    }

    /**
     * 使用普通聊天模型安全改写风险输出。
     */
    private String rewriteUnsafeChatOutput(String unsafeOutput) {
        ChatResponse response = invokeChatWithRetry(
                buildSafetySystemPrompt(),
                "请安全改写下面的模型输出，保留有帮助的信息，删除或概括不安全细节。"
                        + "只返回改写后的内容。\n\n"
                        + unsafeOutput
        );
        return response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null
                ? null
                : response.getResult().getOutput().getText();
    }

    /**
     * 构建输入审核未放行时的普通聊天响应。
     */
    private MiniMaxChatResponseVo buildModerationChatResponse() {
        MiniMaxChatResponseVo responseVo = new MiniMaxChatResponseVo();
        responseVo.setContent(this.moderationGuard.safeReply());
        responseVo.setProvider("MODERATION");
        responseVo.setDurationMs(0L);
        return responseVo;
    }

    /**
     * 复用公共 Prompt Chat 的 AIRAG 模型配置作为审核模型。
     */
    private String resolveModerationModelId() {
        if (this.promptChatConfigBean != null
                && StringUtils.hasText(this.promptChatConfigBean.getModelId())) {
            return this.promptChatConfigBean.getModelId().trim();
        }
        if (this.promptChatConfigBean == null
                || !StringUtils.hasText(this.promptChatConfigBean.getAppId())
                || this.airagAppMapper == null) {
            return null;
        }
        AiragApp app = this.airagAppMapper.getByIdIgnoreTenant(
                this.promptChatConfigBean.getAppId().trim()
        );
        return app == null || !StringUtils.hasText(app.getModelId())
                ? null
                : app.getModelId().trim();
    }

    /**
     * 下载并上传生成图片到配置的存储类型，失败时回退原始 URL。
     *
     * @param imageUrls 原始图片地址
     * @return 入桶后的图片地址
     */
    private List<String> uploadGeneratedImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<String> uploadedUrls = new ArrayList<>(imageUrls.size());
        for (String imageUrl : imageUrls) {
            if (!StringUtils.hasText(imageUrl)) {
                continue;
            }
            try {
                byte[] imageBytes = downloadBytes(imageUrl);
                String ext = guessImageExtension(imageUrl);
                String uploadedUrl = uploadBinary(imageBytes, miniMaxDemoConfig.getImageUploadBizPath(), ext);
                uploadedUrls.add(uploadedUrl);
            } catch (Exception e) {
                log.warn("上传生图到对象存储失败，回退原图URL。sourceUrl={}, reason={}", imageUrl, e.getMessage());
                uploadedUrls.add(imageUrl);
            }
        }
        return uploadedUrls;
    }

    /**
     * 下载网络文件字节。
     *
     * @param url 文件URL
     * @return 文件字节
     * @throws IOException 下载失败
     */
    private byte[] downloadBytes(String url) throws IOException {
        try (InputStream inputStream = FileDownloadUtils.getDownInputStream(url, "")) {
            if (inputStream == null) {
                throw new IOException("download stream is empty");
            }
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                return outputStream.toByteArray();
            }
        }
    }

    /**
     * 将十六进制音频上传到存储。
     *
     * @param audioHex 十六进制音频
     * @return 上传后的URL
     */
    private String uploadAudioHex(String audioHex) {
        if (!StringUtils.hasText(audioHex)) {
            return null;
        }
        byte[] audioBytes = hexToBytes(audioHex);
        return uploadBinary(audioBytes, miniMaxDemoConfig.getAudioUploadBizPath(), "mp3");
    }

    /**
     * 十六进制字符串转字节数组。
     *
     * @param hexValue 十六进制字符串
     * @return 字节数组
     */
    private byte[] hexToBytes(String hexValue) {
        String cleanHex = hexValue.trim();
        if (cleanHex.startsWith("0x") || cleanHex.startsWith("0X")) {
            cleanHex = cleanHex.substring(2);
        }
        cleanHex = cleanHex.replaceAll("\\s+", "");
        if ((cleanHex.length() & 1) == 1) {
            cleanHex = "0" + cleanHex;
        }
        int length = cleanHex.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int high = Character.digit(cleanHex.charAt(i), 16);
            int low = Character.digit(cleanHex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new JeecgBootBizTipException("MiniMax tts audio format is invalid");
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }

    /**
     * 按 uploadType 上传二进制数据。
     *
     * @param data 文件内容
     * @param bizPath 业务路径
     * @param extension 扩展名
     * @return 上传后URL
     */
    private String uploadBinary(byte[] data, String bizPath, String extension) {
        if (data == null || data.length == 0) {
            throw new JeecgBootBizTipException("generated media is empty");
        }
        String cleanExt = StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ROOT) : "bin";
        String cleanBizPath = StringUtils.hasText(bizPath) ? bizPath.trim().replace("\\", "/") : "airag/minimax";
        if (cleanBizPath.startsWith("/")) {
            cleanBizPath = cleanBizPath.substring(1);
        }
        if (cleanBizPath.endsWith("/")) {
            cleanBizPath = cleanBizPath.substring(0, cleanBizPath.length() - 1);
        }
        String objectKey = cleanBizPath + "/" + UUID.randomUUID() + "." + cleanExt;
        String uploadType = jeecgBaseConfig.getUploadType();
        try {
            if (CommonConstant.UPLOAD_TYPE_LOCAL.equals(uploadType)) {
                Path path = jeecgBaseConfig.getPath();
                String uploadRoot = path == null ? null : path.getUpload();
                if (!StringUtils.hasText(uploadRoot)) {
                    throw new JeecgBootBizTipException("local upload path is not configured");
                }
                File target = new File(uploadRoot + File.separator + objectKey.replace("/", File.separator));
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new JeecgBootBizTipException("failed to create local upload directory");
                }
                FileCopyUtils.copy(data, target);
                return buildLocalStaticFileUrl(objectKey);
            }
            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                if (CommonConstant.UPLOAD_TYPE_MINIO.equals(uploadType)) {
                    return MinioUtil.upload(inputStream, objectKey);
                }
                if (CommonConstant.UPLOAD_TYPE_R2.equals(uploadType)) {
                    return CloudflareR2Util.upload(inputStream, objectKey);
                }
                if (CommonConstant.UPLOAD_TYPE_OSS.equals(uploadType)) {
                    return OssBootUtil.upload(inputStream, objectKey);
                }
            }
            throw new JeecgBootBizTipException("unsupported upload type: " + uploadType);
        } catch (JeecgBootBizTipException e) {
            throw e;
        } catch (Exception e) {
            throw new JeecgBootBizTipException("upload generated media failed: " + e.getMessage());
        }
    }

    /**
     * 构建本地上传文件可访问路径（不再返回未替换的 #{domainURL} 占位符）。
     *
     * @param objectKey 存储对象 key
     * @return 静态访问路径
     */
    private String buildLocalStaticFileUrl(String objectKey) {
        String safeObjectKey = objectKey == null ? "" : objectKey.replace("\\", "/");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String normalizedContextPath = normalizeContextPath(contextPath);
        return normalizedContextPath + "/sys/common/static/" + safeObjectKey;
    }

    /**
     * 规范化 context-path：空或 "/" 返回空串，非空保证前导 "/" 且无尾部 "/"
     *
     * @param contextPath 原始 context-path
     * @return 规范化后的 context-path
     */
    private String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath)) {
            return "";
        }
        String value = contextPath.trim();
        if ("/".equals(value)) {
            return "";
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 推断图片扩展名。
     *
     * @param imageUrl 图片URL
     * @return 扩展名
     */
    private String guessImageExtension(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return "png";
        }
        String path = imageUrl;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == path.length() - 1) {
            return "png";
        }
        String ext = path.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (isSupportedImageExtension(ext)) {
            return ext;
        }
        return "png";
    }

    /**
     * 判断是否支持当前图片扩展名。
     *
     * @param ext 扩展名
     * @return 是否支持
     */
    private boolean isSupportedImageExtension(String ext) {
        return "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)
                || "webp".equals(ext) || "gif".equals(ext) || "bmp".equals(ext);
    }

    /**
     * 重试前等待。
     */
    private void sleepBeforeRetry() {
        int backoff = Math.max(miniMaxDemoConfig.getRetryBackoffMs(), 0);
        if (backoff <= 0) {
            return;
        }
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 统计 Unicode 码点长度，避免 emoji/代理对导致的长度感知偏差。
     *
     * @param text 输入文本
     * @return 码点长度
     */
    private int countCodePoints(String text) {
        return text == null ? 0 : text.codePointCount(0, text.length());
    }
}
