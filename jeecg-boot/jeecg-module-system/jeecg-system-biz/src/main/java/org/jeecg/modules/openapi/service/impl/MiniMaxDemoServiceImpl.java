package org.jeecg.modules.openapi.service.impl;

import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.FileDownloadUtils;
import org.jeecg.common.util.MinioUtil;
import org.jeecg.common.util.oss.OssBootUtil;
import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.config.vo.Path;
import org.jeecg.modules.openapi.config.MiniMaxDemoConfigBean;
import org.jeecg.modules.openapi.config.MiniMaxDemoGuardConfigBean;
import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.IMiniMaxMediaService;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

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
public class MiniMaxDemoServiceImpl implements IMiniMaxDemoService {

    private final ChatClient chatClient;
    private final IMiniMaxMediaService miniMaxMediaService;
    private final MiniMaxDemoGuardConfigBean guardConfig;
    private final MiniMaxDemoConfigBean miniMaxDemoConfig;
    private final JeecgBaseConfig jeecgBaseConfig;

    public MiniMaxDemoServiceImpl(ChatClient.Builder chatClientBuilder,
                                  IMiniMaxMediaService miniMaxMediaService,
                                  MiniMaxDemoGuardConfigBean guardConfig,
                                  MiniMaxDemoConfigBean miniMaxDemoConfig,
                                  JeecgBaseConfig jeecgBaseConfig) {
        this.chatClient = chatClientBuilder.build();
        this.miniMaxMediaService = miniMaxMediaService;
        this.guardConfig = guardConfig;
        this.miniMaxDemoConfig = miniMaxDemoConfig;
        this.jeecgBaseConfig = jeecgBaseConfig;
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
        if (requestDto.getPrompt().length() > guardConfig.getMaxChatChars()) {
            throw new JeecgBootBizTipException("prompt长度超过限制");
        }
        String content = invokeChatWithRetry(requestDto.getPrompt());
        MiniMaxChatResponseVo responseVo = new MiniMaxChatResponseVo();
        responseVo.setContent(content);
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
        if (!StringUtils.hasText(requestDto.getText())) {
            throw new JeecgBootBizTipException("text不能为空");
        }
        if (!StringUtils.hasText(requestDto.getVoiceId())) {
            throw new JeecgBootBizTipException("voiceId不能为空");
        }
        if (requestDto.getText().length() > guardConfig.getMaxTtsChars()) {
            throw new JeecgBootBizTipException("text长度超过限制");
        }
        String audioHex = miniMaxMediaService.textToSpeech(requestDto.getText(), requestDto.getVoiceId());
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
        if (!StringUtils.hasText(requestDto.getPrompt())) {
            throw new JeecgBootBizTipException("prompt不能为空");
        }
        if (requestDto.getPrompt().length() > guardConfig.getMaxImagePromptChars()) {
            throw new JeecgBootBizTipException("prompt长度超过限制");
        }
        List<String> imageUrls = miniMaxMediaService.generateImage(requestDto.getPrompt());
        MiniMaxImageResponseVo responseVo = new MiniMaxImageResponseVo();
        responseVo.setOriginalImageUrls(imageUrls);
        if (miniMaxDemoConfig.isUploadGeneratedMedia()) {
            responseVo.setImageUrls(uploadGeneratedImages(imageUrls));
        } else {
            responseVo.setImageUrls(imageUrls);
        }
        return responseVo;
    }

    /**
     * 调用 Spring AI ChatClient，并执行重试。
     *
     * @param prompt 输入提示词
     * @return 模型输出文本
     */
    private String invokeChatWithRetry(String prompt) {
        int maxAttempts = Math.max(miniMaxDemoConfig.getRetryMaxAttempts(), 1);
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return chatClient.prompt(prompt).call().content();
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
            } catch (Exception ignored) {
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
                return "#{domainURL}/" + objectKey;
            }
            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                if (CommonConstant.UPLOAD_TYPE_MINIO.equals(uploadType)) {
                    return MinioUtil.upload(inputStream, objectKey);
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
}
