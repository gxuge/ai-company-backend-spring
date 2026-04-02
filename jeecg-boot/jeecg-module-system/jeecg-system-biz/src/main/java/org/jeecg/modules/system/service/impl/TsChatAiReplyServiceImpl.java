package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect.CheckTsChatSessionOwnership;
import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;
import org.jeecg.modules.system.dto.tschatsession.TsChatAiReplyDto;
import org.jeecg.modules.system.entity.TsChatMessage;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.entity.TsUserVoiceConfig;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.mapper.TsChatMessageAttachmentMapper;
import org.jeecg.modules.system.mapper.TsChatMessageMapper;
import org.jeecg.modules.system.mapper.TsChatSessionMapper;
import org.jeecg.modules.system.mapper.TsUserVoiceConfigMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
import org.jeecg.modules.system.service.ITsChatAiReplyService;
import org.jeecg.modules.system.vo.tschatsession.TsChatAiReplyVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class TsChatAiReplyServiceImpl implements ITsChatAiReplyService {

    /** 历史上下文最大字符数，防止 prompt 过长导致模型响应不稳定。 */
    private static final int MAX_HISTORY_PROMPT_CHARS = 2500;
    /** 消息发送者类型：用户。 */
    private static final String SENDER_TYPE_USER = "user";
    /** 消息发送者类型：AI 角色。 */
    private static final String SENDER_TYPE_ROLE = "role";
    /** 消息发送者类型：系统。 */
    private static final String SENDER_TYPE_SYSTEM = "system";
    /** 展示用角色名：用户。 */
    private static final String ROLE_NAME_USER = "用户";
    /** 展示用角色名：系统。 */
    private static final String ROLE_NAME_SYSTEM = "系统";
    /** 展示用角色名：AI。 */
    private static final String ROLE_NAME_AI = "AI";
    /** 发送者名称：用户。 */
    private static final String SENDER_NAME_USER = "用户";
    /** 发送者名称：AI 伴侣。 */
    private static final String SENDER_NAME_ASSISTANT = "AI伴侣";
    /** 消息类型：文本。 */
    private static final String MESSAGE_TYPE_TEXT = "text";
    /** 消息类型：语音。 */
    private static final String MESSAGE_TYPE_VOICE = "voice";
    /** 生成状态：成功。 */
    private static final String GENERATE_STATUS_SUCCESS = "success";
    /** 附件类型：语音。 */
    private static final String FILE_TYPE_VOICE = "voice";
    /** 语音 MIME 类型。 */
    private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
    /** 音频文件名前缀。 */
    private static final String AUDIO_FILE_PREFIX = "ai-reply-";
    /** 音频文件名后缀。 */
    private static final String AUDIO_FILE_SUFFIX = ".mp3";
    /** Prompt 指令：系统角色设定。 */
    private static final String PROMPT_SYSTEM = "你是一个温柔、自然、简洁的中文AI伴侣，请基于对话上下文回复。";
    /** Prompt 指令：历史对话前缀。 */
    private static final String PROMPT_HISTORY_PREFIX = "历史对话：";
    /** Prompt 指令：用户输入前缀。 */
    private static final String PROMPT_USER_PREFIX = "用户当前消息：";
    /** Prompt 指令：输出格式约束。 */
    private static final String PROMPT_OUTPUT_RULE = "请直接回复可读文本，不要输出JSON。";

    @Resource
    private TsChatMessageMapper tsChatMessageMapper;

    @Resource
    private TsChatMessageAttachmentMapper tsChatMessageAttachmentMapper;

    @Resource
    private TsChatSessionMapper tsChatSessionMapper;

    @Resource
    private TsUserVoiceConfigMapper tsUserVoiceConfigMapper;

    @Resource
    private TsVoiceProfileMapper tsVoiceProfileMapper;

    @Resource
    private IMiniMaxDemoService miniMaxDemoService;

    /**
     * 在会话内完成“用户消息入库 + AI 文本生成 + 语音合成 + 附件落库”的编排流程。
     *
     * @param user 当前登录用户
     * @param sessionId 会话 ID
     * @param request AI 回复请求参数（文本、历史窗口、音色配置）
     * @return AI 回复结果（文本、音频地址、消息与附件主键）
     * @throws JeecgBootException 当用户消息为空、音色不可用或下游未返回可播放地址时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatAiReplyVo> createAiReply(LoginUser user, Long sessionId, TsChatAiReplyDto request) {
        request.applyDefaults();
        String userContent = request.getUserContent() == null ? null : request.getUserContent().trim();
        if (!StringUtils.hasText(userContent)) {
            throw new JeecgBootException("用户消息不能为空");
        }

        TsChatSession session = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        List<TsChatMessage> historyMessages = tsChatMessageMapper.selectRecentMessages(sessionId, request.getHistoryCount());

        TsChatMessage userMessage = new TsChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setSenderType(SENDER_TYPE_USER);
        userMessage.setSenderName(SENDER_NAME_USER);
        userMessage.setMessageType(MESSAGE_TYPE_TEXT);
        userMessage.setContentText(userContent);
        userMessage.setGenerateStatus(GENERATE_STATUS_SUCCESS);
        userMessage.setSeqNo(tsChatMessageMapper.selectNextSeqNoForUpdate(sessionId));
        userMessage.setCreatedAt(new Date());
        tsChatMessageMapper.insert(userMessage);

        // 组装历史上下文，按时序从旧到新拼接，达到上限后截断。
        StringBuilder historyBuilder = new StringBuilder();
        if (historyMessages != null && !historyMessages.isEmpty()) {
            List<TsChatMessage> orderedMessages = new ArrayList<>(historyMessages);
            Collections.reverse(orderedMessages);
            for (TsChatMessage message : orderedMessages) {
                if (message == null || !StringUtils.hasText(message.getContentText())) {
                    continue;
                }
                String roleName = ROLE_NAME_AI;
                if (StringUtils.hasText(message.getSenderType())) {
                    String normalizedSenderType = message.getSenderType().trim().toLowerCase(Locale.ROOT);
                    if (SENDER_TYPE_USER.equals(normalizedSenderType)) {
                        roleName = ROLE_NAME_USER;
                    } else if (SENDER_TYPE_SYSTEM.equals(normalizedSenderType)) {
                        roleName = ROLE_NAME_SYSTEM;
                    }
                }
                String line = "【" + roleName + "】" + message.getContentText().trim() + "\n";
                if (historyBuilder.length() + line.length() > MAX_HISTORY_PROMPT_CHARS) {
                    break;
                }
                historyBuilder.append(line);
            }
        }
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(PROMPT_SYSTEM).append("\n");
        if (historyBuilder.length() > 0) {
            promptBuilder.append(PROMPT_HISTORY_PREFIX).append("\n").append(historyBuilder);
        }
        promptBuilder.append(PROMPT_USER_PREFIX).append("\n").append(userContent).append("\n");
        promptBuilder.append(PROMPT_OUTPUT_RULE);

        MiniMaxChatRequestDto chatRequest = new MiniMaxChatRequestDto();
        chatRequest.setPrompt(promptBuilder.toString());
        MiniMaxChatResponseVo chatResponse = miniMaxDemoService.chat(chatRequest);
        String assistantContent = chatResponse == null ? null : chatResponse.getContent();
        if (!StringUtils.hasText(assistantContent)) {
            throw new JeecgBootException("AI回复为空，请稍后重试");
        }
        assistantContent = assistantContent.trim();

        Long resolvedVoiceProfileId = request.getVoiceProfileId();
        String resolvedVoiceId;
        if (StringUtils.hasText(request.getVoiceId())) {
            resolvedVoiceId = request.getVoiceId().trim();
        } else {
            Long voiceProfileId = request.getVoiceProfileId();
            if (voiceProfileId == null) {
                TsUserVoiceConfig userVoiceConfig = tsUserVoiceConfigMapper.selectByUserId(user.getId());
                if (userVoiceConfig != null) {
                    voiceProfileId = userVoiceConfig.getSelectedVoiceProfileId();
                }
            }
            if (voiceProfileId == null) {
                throw new JeecgBootException("未找到可用音色，请先在 /sys/ts-user-voice-config/current 设置音色");
            }
            TsVoiceProfile voiceProfile = tsVoiceProfileMapper.selectActiveById(voiceProfileId);
            if (voiceProfile == null) {
                throw new JeecgBootException("音色不存在或已停用，请重新选择");
            }
            if (!StringUtils.hasText(voiceProfile.getProviderVoiceId())) {
                throw new JeecgBootException("当前音色未配置 providerVoiceId，无法进行语音合成");
            }
            resolvedVoiceId = voiceProfile.getProviderVoiceId().trim();
            resolvedVoiceProfileId = voiceProfile.getId();
        }

        MiniMaxTtsRequestDto ttsRequest = new MiniMaxTtsRequestDto();
        ttsRequest.setText(assistantContent);
        ttsRequest.setVoiceId(resolvedVoiceId);
        MiniMaxTtsResponseVo ttsResponse = miniMaxDemoService.tts(ttsRequest);
        String audioUrl = ttsResponse == null ? null : ttsResponse.getAudioUrl();
        if (!StringUtils.hasText(audioUrl)) {
            throw new JeecgBootException("语音生成成功但未返回可播放地址，请检查 AIRAG_MINIMAX_UPLOAD_GENERATED_MEDIA 配置");
        }

        JSONObject assistantContentJson = new JSONObject();
        assistantContentJson.put("audioUrl", audioUrl);
        assistantContentJson.put("voiceId", resolvedVoiceId);
        assistantContentJson.put("mimeType", MIME_TYPE_AUDIO_MPEG);

        TsChatMessage assistantMessage = new TsChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setSenderType(SENDER_TYPE_ROLE);
        assistantMessage.setSenderName(SENDER_NAME_ASSISTANT);
        assistantMessage.setMessageType(MESSAGE_TYPE_VOICE);
        assistantMessage.setContentText(assistantContent);
        assistantMessage.setContentJson(assistantContentJson.toJSONString());
        assistantMessage.setReplyToMessageId(userMessage.getId());
        assistantMessage.setGenerateStatus(GENERATE_STATUS_SUCCESS);
        assistantMessage.setSeqNo(tsChatMessageMapper.selectNextSeqNoForUpdate(sessionId));
        assistantMessage.setCreatedAt(new Date());
        tsChatMessageMapper.insert(assistantMessage);

        Long estimatedAudioSize = null;
        String audioHex = ttsResponse == null ? null : ttsResponse.getAudioHex();
        if (StringUtils.hasText(audioHex)) {
            String cleanHex = audioHex.trim();
            if (cleanHex.startsWith("0x") || cleanHex.startsWith("0X")) {
                cleanHex = cleanHex.substring(2);
            }
            cleanHex = cleanHex.replaceAll("\\s+", "");
            if (!cleanHex.isEmpty()) {
                if ((cleanHex.length() & 1) == 1) {
                    cleanHex = "0" + cleanHex;
                }
                estimatedAudioSize = (long) cleanHex.length() / 2L;
            }
        }

        TsChatMessageAttachment attachment = new TsChatMessageAttachment();
        attachment.setMessageId(assistantMessage.getId());
        attachment.setFileType(FILE_TYPE_VOICE);
        attachment.setFileUrl(audioUrl);
        attachment.setFileName(AUDIO_FILE_PREFIX + assistantMessage.getId() + AUDIO_FILE_SUFFIX);
        attachment.setFileSize(estimatedAudioSize);
        attachment.setDurationSec(null);
        attachment.setMimeType(MIME_TYPE_AUDIO_MPEG);
        attachment.setCreatedAt(new Date());
        tsChatMessageAttachmentMapper.insert(attachment);

        Date now = new Date();
        session.setLastMessageId(assistantMessage.getId());
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
        tsChatSessionMapper.updateById(session);

        TsChatAiReplyVo response = new TsChatAiReplyVo();
        response.setSessionId(sessionId);
        response.setUserMessageId(userMessage.getId());
        response.setAssistantMessageId(assistantMessage.getId());
        response.setAttachmentId(attachment.getId());
        response.setVoiceProfileId(resolvedVoiceProfileId);
        response.setVoiceId(resolvedVoiceId);
        response.setContentText(assistantContent);
        response.setAudioUrl(audioUrl);
        response.setAudioFileSize(attachment.getFileSize());
        response.setDurationSec(attachment.getDurationSec());
        response.setMimeType(attachment.getMimeType());
        response.setCreatedAt(assistantMessage.getCreatedAt());
        return Result.OK("生成成功", response);
    }
}
