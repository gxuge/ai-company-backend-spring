package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.modules.airag.usage.model.AiUsageFinishRequest;
import org.jeecg.modules.airag.usage.model.AiUsageMetricValue;
import org.jeecg.modules.airag.usage.model.AiUsageStartRequest;
import org.jeecg.modules.airag.usage.service.AiUsageRecorderService;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect.CheckTsChatSessionOwnership;
import org.jeecg.modules.openapi.dto.MiniMaxChatRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.openapi.vo.MiniMaxChatResponseVo;
import org.jeecg.modules.system.dto.tschatsession.TsChatAiReplyDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatMessageTtsDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatReplySuggestionsDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatTtsSynthesizeDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatTemplateReplyDto;
import org.jeecg.modules.system.entity.TsChatMessage;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.entity.TsStoryRoleRel;
import org.jeecg.modules.system.entity.TsUserVoiceConfig;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.mapper.TsChatMessageAttachmentMapper;
import org.jeecg.modules.system.mapper.TsChatMessageMapper;
import org.jeecg.modules.system.mapper.TsChatSessionMapper;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.mapper.TsStoryRoleRelMapper;
import org.jeecg.modules.system.mapper.TsUserVoiceConfigMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
import org.jeecg.modules.system.monitor.TsAiLogCollector;
import org.jeecg.modules.system.monitor.TsMultimodalUsageRecorder;
import org.jeecg.modules.system.service.ITsChatAiReplyService;
import org.jeecg.modules.system.service.ITsChatTtsGatewayService;
import org.jeecg.modules.system.util.ChatGenerateSnapshotUtil;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.jeecg.modules.system.util.TsPromptLanguageInjector;
import org.jeecg.modules.system.vo.tschatsession.TsChatAiReplyVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatMessageTtsVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatReplySuggestionsVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatTtsResultVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatTemplateReplyVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
    /** 候选回复模板编码。 */
    private static final String PROMPT_CODE_REPLY_SUGGESTIONS = "chat_session_reply_suggestions_multi_role";
    /** 多角色聊天模板编码。 */
    private static final String PROMPT_CODE_TEMPLATE_REPLY = "chat_session_reply_multi_role";
    /** 候选回复模板版本。 */
    private static final String PROMPT_VERSION = "v1";
    /** 多角色聊天模板版本。 */
    private static final String PROMPT_VERSION_TEMPLATE_REPLY = "v1";
    /** 候选回复快照缓存前缀。 */
    private static final String REDIS_SNAPSHOT_PREFIX = "ts:chat:generate:snapshot:";
    /** 候选回复快照缓存 TTL（小时）。 */
    private static final long REDIS_SNAPSHOT_TTL_HOURS = 72L;
    /** 固定返回候选条数。 */
    private static final int FIXED_SUGGESTION_COUNT = 3;
    /** 单条候选最大长度，防止模型超长输出影响前端。 */
    private static final int MAX_SUGGESTION_LENGTH = 64;
    /** 候选回复兜底文案 1。 */
    private static final String FALLBACK_SUGGESTION_1 = "你刚刚那句我有点在意，能多说一点吗？";
    /** 候选回复兜底文案 2。 */
    private static final String FALLBACK_SUGGESTION_2 = "那你更希望我现在怎么回应你？";
    /** 候选回复兜底文案 3。 */
    private static final String FALLBACK_SUGGESTION_3 = "我们先从最在意的那一件事聊起吧。";
    /** TTS 中默认剔除的括号动作/心理活动。 */
    private static final Pattern TTS_BRACKET_PATTERN = Pattern.compile("（[^（）]*）|\\([^()]*\\)");
    /** TTS 文本中的连续空白压缩规则。 */
    private static final Pattern TTS_MULTI_SPACE_PATTERN = Pattern.compile("[\\t\\x0B\\f\\r ]+");

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
    private TsRoleMapper tsRoleMapper;

    @Resource
    private TsStoryMapper tsStoryMapper;
    @Resource
    private TsStoryRoleRelMapper tsStoryRoleRelMapper;

    @Resource
    private IMiniMaxDemoService miniMaxDemoService;
    @Resource
    private IPromptChatService promptChatService;

    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private ITsChatTtsGatewayService tsChatTtsGatewayService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TsAiLogCollector tsAiLogCollector;
    @Resource
    private AiUsageRecorderService aiUsageRecorderService;
    @Resource
    private TsMultimodalUsageRecorder multimodalUsageRecorder;

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
        chatRequest.setPrompt(TsPromptLanguageInjector.inject(promptBuilder.toString()));
        logPlainChatRequest(sessionId, request, historyMessages, userContent, chatRequest.getPrompt());
        String textInvocationId = UUIDGenerator.generate();
        Date textStartedAt = new Date();
        startPlainChatUsage(textInvocationId, user, sessionId, userMessage.getId(), textStartedAt);
        MiniMaxChatResponseVo chatResponse;
        try {
            chatResponse = miniMaxDemoService.chat(chatRequest);
            finishPlainChatUsage(textInvocationId, chatResponse, textStartedAt, null);
        } catch (RuntimeException ex) {
            finishPlainChatUsage(textInvocationId, null, textStartedAt, ex);
            throw ex;
        }
        String assistantContent = chatResponse == null ? null : chatResponse.getContent();
        logPlainChatResponse(assistantContent);
        if (!StringUtils.hasText(assistantContent)) {
            throw new JeecgBootException("AI回复为空，请稍后重试");
        }
        assistantContent = assistantContent.trim();

        boolean shouldGenerateVoice = Boolean.TRUE.equals(request.getGenerateVoice());
        VoiceSelection voiceSelection = shouldGenerateVoice
                ? resolveVoiceSelection(user.getId(), request.getVoiceProfileId(), request.getVoiceId())
                : null;
        Long resolvedVoiceProfileId = voiceSelection == null ? null : voiceSelection.getVoiceProfileId();
        String resolvedVoiceId = voiceSelection == null ? null : voiceSelection.getVoiceId();
        String voiceMatchSource = voiceSelection == null ? null : voiceSelection.getMatchSource();

        String audioUrl = null;
        TsChatTtsResultVo ttsResult = null;
        JSONObject assistantContentJson = null;
        if (shouldGenerateVoice) {
            TsChatTtsSynthesizeDto ttsRequest = new TsChatTtsSynthesizeDto();
            String ttsText = sanitizeTtsText(assistantContent);
            ttsRequest.setText(ttsText);
            ttsRequest.setVoiceId(resolvedVoiceId);
            ttsRequest.setVoiceProfileId(resolvedVoiceProfileId);
            ttsRequest.setSpeed(request.getSpeed());
            ttsRequest.setPitch(request.getPitch());
            ttsRequest.setVolume(request.getVolume());
            logTtsRequest(ttsRequest, voiceMatchSource);
            ttsResult = multimodalUsageRecorder.recordTts(
                    user == null ? null : user.getId(),
                    sessionId,
                    userMessage.getId(),
                    ttsRequest.getText(),
                    () -> tsChatTtsGatewayService.synthesizeForChat(ttsRequest)
            );
            audioUrl = ttsResult == null ? null : ttsResult.getAudioUrl();
            if (!StringUtils.hasText(resolvedVoiceId) && ttsResult != null && StringUtils.hasText(ttsResult.getVoiceId())) {
                resolvedVoiceId = ttsResult.getVoiceId();
            }
            logTtsResponse(ttsResult);
            if (!StringUtils.hasText(audioUrl)) {
                throw new JeecgBootException("语音生成成功但未返回可播放地址，请检查当前语音模型配置");
            }

            assistantContentJson = new JSONObject();
            assistantContentJson.put("audioUrl", audioUrl);
            assistantContentJson.put("audioCacheKey", ttsResult == null ? null : ttsResult.getCacheKey());
            assistantContentJson.put("voiceId", resolvedVoiceId);
            assistantContentJson.put("voiceProfileId", resolvedVoiceProfileId);
            assistantContentJson.put("matchSource", voiceMatchSource);
            assistantContentJson.put("ttsText", ttsRequest.getText());
            assistantContentJson.put("speed", request.getSpeed());
            assistantContentJson.put("pitch", request.getPitch());
            assistantContentJson.put("volume", request.getVolume());
            assistantContentJson.put("mimeType", ttsResult == null ? MIME_TYPE_AUDIO_MPEG : ttsResult.getMimeType());
            assistantContentJson.put("audioFileSize", ttsResult == null ? null : ttsResult.getFileSize());
            assistantContentJson.put("durationSec", ttsResult == null ? null : ttsResult.getDurationSec());
        }

        TsChatMessage assistantMessage = new TsChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setSenderType(SENDER_TYPE_ROLE);
        assistantMessage.setSenderName(SENDER_NAME_ASSISTANT);
        assistantMessage.setMessageType(shouldGenerateVoice ? MESSAGE_TYPE_VOICE : MESSAGE_TYPE_TEXT);
        assistantMessage.setContentText(assistantContent);
        assistantMessage.setContentJson(assistantContentJson == null ? null : assistantContentJson.toJSONString());
        assistantMessage.setReplyToMessageId(userMessage.getId());
        assistantMessage.setGenerateStatus(GENERATE_STATUS_SUCCESS);
        assistantMessage.setSeqNo(tsChatMessageMapper.selectNextSeqNoForUpdate(sessionId));
        assistantMessage.setCreatedAt(new Date());
        tsChatMessageMapper.insert(assistantMessage);

        TsChatMessageAttachment attachment = null;
        if (shouldGenerateVoice) {
            attachment = new TsChatMessageAttachment();
            attachment.setMessageId(assistantMessage.getId());
            attachment.setFileType(FILE_TYPE_VOICE);
            attachment.setFileUrl(audioUrl);
            attachment.setFileName(AUDIO_FILE_PREFIX + assistantMessage.getId() + AUDIO_FILE_SUFFIX);
            attachment.setFileSize(ttsResult == null ? null : ttsResult.getFileSize());
            attachment.setDurationSec(ttsResult == null ? null : ttsResult.getDurationSec());
            attachment.setMimeType(ttsResult == null ? MIME_TYPE_AUDIO_MPEG : ttsResult.getMimeType());
            attachment.setCreatedAt(new Date());
            tsChatMessageAttachmentMapper.insert(attachment);
        }

        Date now = new Date();
        session.setLastMessageId(assistantMessage.getId());
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
        tsChatSessionMapper.updateById(session);

        TsChatAiReplyVo response = new TsChatAiReplyVo();
        response.setSessionId(sessionId);
        response.setUserMessageId(userMessage.getId());
        response.setAssistantMessageId(assistantMessage.getId());
        response.setAttachmentId(attachment == null ? null : attachment.getId());
        response.setVoiceProfileId(resolvedVoiceProfileId);
        response.setVoiceId(resolvedVoiceId);
        response.setContentText(assistantContent);
        response.setAudioUrl(audioUrl);
        response.setAudioCacheKey(ttsResult == null ? null : ttsResult.getCacheKey());
        response.setAudioFileSize(attachment == null ? null : attachment.getFileSize());
        response.setDurationSec(attachment == null ? null : attachment.getDurationSec());
        response.setMimeType(attachment == null ? null : attachment.getMimeType());
        response.setCreatedAt(assistantMessage.getCreatedAt());
        return Result.OK("生成成功", response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatTemplateReplyVo> createTemplateAiReply(LoginUser user, Long sessionId, TsChatTemplateReplyDto request) {
        TsChatTemplateReplyDto dto = request == null ? new TsChatTemplateReplyDto() : request;
        dto.applyDefaults();
        String userInput = PromptRuntimeUtil.trimToNull(dto.getUserInput());
        if (!StringUtils.hasText(userInput)) {
            throw new JeecgBootException("userInput不能为空");
        }

        TsChatSession session = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        if (session == null) {
            throw new JeecgBootException("会话不存在或无权限访问");
        }

        Long resolvedActiveRoleId = dto.getActiveRoleId() != null ? dto.getActiveRoleId() : resolveSessionActiveRoleId(session);
        if (resolvedActiveRoleId == null) {
            throw new JeecgBootException("当前会话未指定发言角色，请先传activeRoleId");
        }

        TsRole activeRole = tsRoleMapper.selectOwned(resolvedActiveRoleId, user.getId());
        if (activeRole == null) {
            throw new JeecgBootException("activeRoleId不存在或无权限访问");
        }

        List<TsChatMessage> historyMessages = tsChatMessageMapper.selectRecentMessages(sessionId, dto.getHistoryCount());
        String recentMessagesBlock = buildRecentMessagesBlock(historyMessages);
        String lastAssistantMessage = resolveLastAssistantMessage(user.getId(), sessionId, dto.getLastAssistantMessageId());

        TsStory story = null;
        if (session.getStoryId() != null) {
            story = tsStoryMapper.selectOwned(session.getStoryId(), user.getId());
        }

        String otherRolesBlock = buildOtherRolesBlock(user.getId(), story == null ? null : story.getId(), resolvedActiveRoleId);

        TsChatMessage userMessage = new TsChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setSenderType(SENDER_TYPE_USER);
        userMessage.setSenderName(SENDER_NAME_USER);
        userMessage.setMessageType(MESSAGE_TYPE_TEXT);
        userMessage.setContentText(userInput);
        userMessage.setGenerateStatus(GENERATE_STATUS_SUCCESS);
        userMessage.setSeqNo(tsChatMessageMapper.selectNextSeqNoForUpdate(sessionId));
        userMessage.setCreatedAt(new Date());
        tsChatMessageMapper.insert(userMessage);

        Map<String, String> variables = new HashMap<>();
        variables.put("role_name", PromptRuntimeUtil.nullableToken(activeRole.getRoleName()));
        variables.put("gender", PromptRuntimeUtil.nullableToken(activeRole.getGender()));
        variables.put("occupation", PromptRuntimeUtil.nullableToken(activeRole.getOccupation()));
        variables.put("background_story", PromptRuntimeUtil.nullableToken(activeRole.getBackgroundStory()));
        variables.put("other_roles_block", PromptRuntimeUtil.nullableToken(otherRolesBlock));
        variables.put("title", PromptRuntimeUtil.nullableToken(story == null ? null : story.getTitle()));
        variables.put("story_intro", PromptRuntimeUtil.nullableToken(story == null ? null : story.getStoryIntro()));
        variables.put("story_setting", PromptRuntimeUtil.nullableToken(story == null ? null : story.getStorySetting()));
        variables.put("site_setting", PromptRuntimeUtil.nullableToken(story == null ? null : story.getSiteSetting()));
        variables.put("plot_outline", PromptRuntimeUtil.nullableToken(story == null ? null : story.getPlotOutline()));
        variables.put("last_assistant_message", PromptRuntimeUtil.nullableToken(lastAssistantMessage));
        variables.put("recent_messages_block", PromptRuntimeUtil.nullableToken(recentMessagesBlock));
        variables.put("user_input", PromptRuntimeUtil.nullableToken(userInput));

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                PROMPT_CODE_TEMPLATE_REPLY, PROMPT_VERSION_TEMPLATE_REPLY, variables);
        TsPromptLanguageInjector.inject(promptSections);
        String renderedPrompt = promptSections.getRenderedPrompt();
        String assistantContent = promptChatService.chat(renderedPrompt);
        assistantContent = PromptRuntimeUtil.trimToNull(assistantContent);
        if (!StringUtils.hasText(assistantContent)) {
            throw new JeecgBootException("AI回复为空，请稍后重试");
        }

        TsChatMessage assistantMessage = new TsChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setSenderType(SENDER_TYPE_ROLE);
        assistantMessage.setSenderName(PromptRuntimeUtil.firstNonBlank(activeRole.getRoleName(), SENDER_NAME_ASSISTANT));
        assistantMessage.setMessageType(MESSAGE_TYPE_TEXT);
        assistantMessage.setContentText(assistantContent);
        assistantMessage.setReplyToMessageId(userMessage.getId());
        assistantMessage.setGenerateStatus(GENERATE_STATUS_SUCCESS);
        assistantMessage.setSeqNo(tsChatMessageMapper.selectNextSeqNoForUpdate(sessionId));
        assistantMessage.setCreatedAt(new Date());
        tsChatMessageMapper.insert(assistantMessage);

        Date now = new Date();
        session.setLastMessageId(assistantMessage.getId());
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
        tsChatSessionMapper.updateById(session);

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "template-reply");
        snapshot.put("promptCode", PROMPT_CODE_TEMPLATE_REPLY);
        snapshot.put("promptVersion", PROMPT_VERSION_TEMPLATE_REPLY);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("activeRoleId", resolvedActiveRoleId);
        snapshot.put("activeRoleName", activeRole.getRoleName());
        snapshot.put("storyId", story == null ? null : story.getId());
        snapshot.put("otherRolesBlock", otherRolesBlock);
        snapshot.put("lastAssistantMessage", lastAssistantMessage);
        snapshot.put("recentMessagesBlock", recentMessagesBlock);
        snapshot.put("userInput", userInput);
        snapshot.put("assistantContent", assistantContent);
        String snapshotKey = ChatGenerateSnapshotUtil.saveSnapshot(
                redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS, "chat-template", user.getId(), snapshot);

        TsChatTemplateReplyVo response = new TsChatTemplateReplyVo();
        response.setSessionId(sessionId);
        response.setActiveRoleId(resolvedActiveRoleId);
        response.setUserMessageId(userMessage.getId());
        response.setAssistantMessageId(assistantMessage.getId());
        response.setActiveRoleName(activeRole.getRoleName());
        response.setContentText(assistantContent);
        response.setPromptCode(PROMPT_CODE_TEMPLATE_REPLY);
        response.setPromptVersion(PROMPT_VERSION_TEMPLATE_REPLY);
        response.setRenderedPrompt(renderedPrompt);
        response.setSnapshotKey(snapshotKey);
        response.setCreatedAt(assistantMessage.getCreatedAt());
        return Result.OK("生成成功", response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatMessageTtsVo> createMessageTts(LoginUser user, Long sessionId, TsChatMessageTtsDto request) {
        TsChatMessageTtsDto dto = request == null ? new TsChatMessageTtsDto() : request;
        dto.applyDefaults();
        TsChatSession session = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        if (session == null) {
            throw new JeecgBootException("会话不存在或无权限访问");
        }

        TsChatMessage message = tsChatMessageMapper.selectOwnedById(dto.getMessageId(), user.getId());
        if (message == null || !sessionId.equals(message.getSessionId())) {
            throw new JeecgBootException("messageId不属于当前会话");
        }
        if (!SENDER_TYPE_ROLE.equalsIgnoreCase(PromptRuntimeUtil.trimToNull(message.getSenderType()))) {
            throw new JeecgBootException("当前消息不是角色回复，无法生成语音");
        }

        String ttsText = sanitizeTtsText(message.getContentText());
        if (!StringUtils.hasText(ttsText)) {
            throw new JeecgBootException("当前消息没有可用于播报的文本");
        }

        VoiceSelection voiceSelection = resolveVoiceSelection(user.getId(), dto.getVoiceProfileId(), dto.getVoiceId());
        TsChatTtsSynthesizeDto ttsRequest = new TsChatTtsSynthesizeDto();
        ttsRequest.setText(ttsText);
        ttsRequest.setVoiceId(voiceSelection.getVoiceId());
        ttsRequest.setVoiceProfileId(voiceSelection.getVoiceProfileId());
        ttsRequest.setSpeed(dto.getSpeed());
        ttsRequest.setPitch(dto.getPitch());
        ttsRequest.setVolume(dto.getVolume());

        TsChatTtsResultVo ttsResult = multimodalUsageRecorder.recordTts(
                user.getId(),
                sessionId,
                message.getId(),
                ttsRequest.getText(),
                () -> tsChatTtsGatewayService.synthesizeForChat(ttsRequest)
        );
        String audioUrl = ttsResult == null ? null : PromptRuntimeUtil.trimToNull(ttsResult.getAudioUrl());
        if (!StringUtils.hasText(audioUrl)) {
            throw new JeecgBootException("语音生成成功但未返回可播放地址");
        }
        String resolvedVoiceId = StringUtils.hasText(voiceSelection.getVoiceId())
                ? voiceSelection.getVoiceId()
                : (ttsResult == null ? null : ttsResult.getVoiceId());

        JSONObject contentJson = parseMessageContentJson(message.getContentJson());
        contentJson.put("audioUrl", audioUrl);
        contentJson.put("audioCacheKey", ttsResult == null ? null : ttsResult.getCacheKey());
        contentJson.put("voiceId", resolvedVoiceId);
        contentJson.put("voiceProfileId", voiceSelection.getVoiceProfileId());
        contentJson.put("matchSource", voiceSelection.getMatchSource());
        contentJson.put("ttsText", ttsText);
        contentJson.put("speed", dto.getSpeed());
        contentJson.put("pitch", dto.getPitch());
        contentJson.put("volume", dto.getVolume());
        contentJson.put("mimeType", ttsResult == null ? MIME_TYPE_AUDIO_MPEG : ttsResult.getMimeType());
        contentJson.put("audioFileSize", ttsResult == null ? null : ttsResult.getFileSize());
        contentJson.put("durationSec", ttsResult == null ? null : ttsResult.getDurationSec());
        message.setContentJson(contentJson.toJSONString());
        tsChatMessageMapper.updateById(message);

        TsChatMessageTtsVo response = new TsChatMessageTtsVo();
        response.setSessionId(sessionId);
        response.setMessageId(message.getId());
        response.setVoiceProfileId(voiceSelection.getVoiceProfileId());
        response.setVoiceId(resolvedVoiceId);
        response.setTtsText(ttsText);
        response.setAudioUrl(audioUrl);
        response.setAudioCacheKey(ttsResult == null ? null : ttsResult.getCacheKey());
        response.setAudioFileSize(ttsResult == null ? null : ttsResult.getFileSize());
        response.setDurationSec(ttsResult == null ? null : ttsResult.getDurationSec());
        response.setMimeType(ttsResult == null ? MIME_TYPE_AUDIO_MPEG : ttsResult.getMimeType());
        response.setCreatedAt(message.getCreatedAt());
        return Result.OK("生成成功", response);
    }

    private String resolveLastAssistantMessage(String userId, Long sessionId, Long lastAssistantMessageId) {
        if (lastAssistantMessageId == null) {
            return null;
        }
        TsChatMessage focusMessage = tsChatMessageMapper.selectOwnedById(lastAssistantMessageId, userId);
        if (focusMessage == null || !sessionId.equals(focusMessage.getSessionId())) {
            throw new JeecgBootException("lastAssistantMessageId不属于当前会话");
        }
        String senderType = PromptRuntimeUtil.trimToNull(focusMessage.getSenderType());
        if (!StringUtils.hasText(senderType)) {
            throw new JeecgBootException("lastAssistantMessageId不是有效的助手消息");
        }
        String normalizedSenderType = senderType.toLowerCase(Locale.ROOT);
        if (!SENDER_TYPE_ROLE.equals(normalizedSenderType) && !SENDER_TYPE_SYSTEM.equals(normalizedSenderType)) {
            throw new JeecgBootException("lastAssistantMessageId不是有效的助手消息");
        }
        return PromptRuntimeUtil.trimToNull(focusMessage.getContentText());
    }

    /**
     * 在会话内生成 3 条可直接发送的候选回复，不落库消息。
     *
     * @param user 当前登录用户
     * @param sessionId 会话 ID
     * @param request 候选回复请求参数
     * @return 候选回复结果
     */
    @Override
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatReplySuggestionsVo> replySuggestions(LoginUser user, Long sessionId, TsChatReplySuggestionsDto request) {
        TsChatReplySuggestionsDto dto = request == null ? new TsChatReplySuggestionsDto() : request;
        dto.applyDefaults();

        TsChatSession session = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        if (session == null) {
            throw new JeecgBootException("会话不存在或无权限访问");
        }

        List<TsChatMessage> historyMessages = tsChatMessageMapper.selectRecentMessages(sessionId, dto.getHistoryCount());
        String recentMessagesBlock = buildRecentMessagesBlock(historyMessages);

        TsStory story = null;
        if (session.getStoryId() != null) {
            story = tsStoryMapper.selectOwned(session.getStoryId(), user.getId());
        }

        Long activeRoleId = resolveSessionActiveRoleId(session);
        TsRole activeRole = activeRoleId == null ? null : tsRoleMapper.selectOwned(activeRoleId, user.getId());
        String otherRolesBlock = buildOtherRolesBlock(user.getId(), story == null ? null : story.getId(), activeRoleId);

        String lastAssistantMessage = null;
        if (dto.getLastAssistantMessageId() != null) {
            TsChatMessage focusMessage = tsChatMessageMapper.selectOwnedById(dto.getLastAssistantMessageId(), user.getId());
            if (focusMessage == null || !sessionId.equals(focusMessage.getSessionId())) {
                throw new JeecgBootException("lastAssistantMessageId不属于当前会话");
            }
            lastAssistantMessage = PromptRuntimeUtil.trimToNull(focusMessage.getContentText());
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("role_name", PromptRuntimeUtil.nullableToken(activeRole == null ? null : activeRole.getRoleName()));
        variables.put("gender", PromptRuntimeUtil.nullableToken(activeRole == null ? null : activeRole.getGender()));
        variables.put("occupation", PromptRuntimeUtil.nullableToken(activeRole == null ? null : activeRole.getOccupation()));
        variables.put("background_story", PromptRuntimeUtil.nullableToken(activeRole == null ? null : activeRole.getBackgroundStory()));
        variables.put("other_roles_block", PromptRuntimeUtil.nullableToken(otherRolesBlock));
        variables.put("title", PromptRuntimeUtil.nullableToken(story == null ? null : story.getTitle()));
        variables.put("story_intro", PromptRuntimeUtil.nullableToken(story == null ? null : story.getStoryIntro()));
        variables.put("story_setting", PromptRuntimeUtil.nullableToken(story == null ? null : story.getStorySetting()));
        variables.put("site_setting", PromptRuntimeUtil.nullableToken(story == null ? null : story.getSiteSetting()));
        variables.put("plot_outline", PromptRuntimeUtil.nullableToken(story == null ? null : story.getPlotOutline()));
        variables.put("last_assistant_message", PromptRuntimeUtil.nullableToken(lastAssistantMessage));
        variables.put("recent_messages_block", PromptRuntimeUtil.nullableToken(recentMessagesBlock));
        variables.put("user_input", PromptRuntimeUtil.nullableToken(dto.getUserDraft()));

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                PROMPT_CODE_REPLY_SUGGESTIONS, PROMPT_VERSION, variables);
        TsPromptLanguageInjector.inject(promptSections);
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(promptChatService, promptSections);

        List<String> suggestions = extractToolCallSuggestions(modelJson);
        if (suggestions.isEmpty()) {
            suggestions.addAll(normalizeSuggestionList(modelJson.get("suggestions")));
        }
        suggestions = ensureFixedSuggestions(suggestions);

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "reply-suggestions");
        snapshot.put("promptCode", PROMPT_CODE_REPLY_SUGGESTIONS);
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        snapshot.put("result", suggestions);
        String snapshotKey = ChatGenerateSnapshotUtil.saveSnapshot(
                redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS, "suggest", user.getId(), snapshot);

        TsChatReplySuggestionsVo response = new TsChatReplySuggestionsVo();
        response.setSessionId(sessionId);
        response.setSuggestions(suggestions);
        response.setPromptCode(PROMPT_CODE_REPLY_SUGGESTIONS);
        response.setPromptVersion(PROMPT_VERSION);
        response.setRenderedPrompt(renderedPrompt);
        response.setSnapshotKey(snapshotKey);
        return Result.OK("生成成功", response);
    }

    private Long resolveSessionActiveRoleId(TsChatSession session) {
        if (session == null) {
            return null;
        }
        if (session.getTargetRoleId() != null) {
            return session.getTargetRoleId();
        }
        if (session.getStoryId() == null) {
            return null;
        }
        List<TsStoryRoleRel> roleRelations = tsStoryRoleRelMapper.selectByStoryId(session.getStoryId());
        if (roleRelations == null || roleRelations.isEmpty()) {
            return null;
        }
        for (TsStoryRoleRel relation : roleRelations) {
            if (relation != null && relation.getRoleId() != null) {
                return relation.getRoleId();
            }
        }
        return null;
    }

    /**
     * 将最近消息拼装成 prompt 上下文文本（按时间从旧到新）。
     */
    private String buildRecentMessagesBlock(List<TsChatMessage> historyMessages) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return null;
        }
        List<TsChatMessage> orderedMessages = new ArrayList<>(historyMessages);
        Collections.reverse(orderedMessages);
        StringBuilder builder = new StringBuilder();
        for (TsChatMessage message : orderedMessages) {
            if (message == null || !StringUtils.hasText(message.getContentText())) {
                continue;
            }
            String roleName = ROLE_NAME_AI;
            String senderType = PromptRuntimeUtil.trimToNull(message.getSenderType());
            if (StringUtils.hasText(senderType)) {
                String normalizedSenderType = senderType.toLowerCase(Locale.ROOT);
                if (SENDER_TYPE_USER.equals(normalizedSenderType)) {
                    roleName = ROLE_NAME_USER;
                } else if (SENDER_TYPE_SYSTEM.equals(normalizedSenderType)) {
                    roleName = ROLE_NAME_SYSTEM;
                }
            }
            if (ROLE_NAME_AI.equals(roleName) && StringUtils.hasText(message.getSenderName())) {
                roleName = message.getSenderName().trim();
            }
            String line = "【" + roleName + "】" + message.getContentText().trim() + "\n";
            if (builder.length() + line.length() > MAX_HISTORY_PROMPT_CHARS) {
                break;
            }
            builder.append(line);
        }
        return PromptRuntimeUtil.trimToNull(builder.toString());
    }

    /**
     * 按故事绑定角色顺序拼接同场其他角色块，供聊天模板作为背景参考。
     */
    private String buildOtherRolesBlock(String userId, Long storyId, Long activeRoleId) {
        if (storyId == null) {
            return null;
        }
        List<TsStoryRoleRel> roleRelations = tsStoryRoleRelMapper.selectByStoryId(storyId);
        if (roleRelations == null || roleRelations.isEmpty()) {
            return null;
        }

        List<Long> candidateRoleIds = new ArrayList<>();
        for (TsStoryRoleRel relation : roleRelations) {
            if (relation == null || relation.getRoleId() == null) {
                continue;
            }
            if (relation.getRoleId().equals(activeRoleId)) {
                continue;
            }
            candidateRoleIds.add(relation.getRoleId());
        }
        if (candidateRoleIds.isEmpty()) {
            return null;
        }

        List<Long> ownedRoleIds = tsRoleMapper.selectOwnedIds(candidateRoleIds, userId);
        if (ownedRoleIds == null || ownedRoleIds.isEmpty()) {
            return null;
        }
        Set<Long> ownedIdSet = new HashSet<>(ownedRoleIds);
        List<TsRole> ownedRoles = tsRoleMapper.selectBatchIds(ownedRoleIds);
        if (ownedRoles == null || ownedRoles.isEmpty()) {
            return null;
        }

        Map<Long, TsRole> roleMap = new HashMap<>();
        for (TsRole role : ownedRoles) {
            if (role != null && role.getId() != null && ownedIdSet.contains(role.getId())) {
                roleMap.put(role.getId(), role);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (Long roleId : candidateRoleIds) {
            TsRole role = roleMap.get(roleId);
            if (role == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("【").append(PromptRuntimeUtil.firstNonBlank(role.getRoleName(), ROLE_NAME_AI)).append("】\n");
            builder.append("性别：").append(PromptRuntimeUtil.nullableToken(role.getGender())).append("\n");
            builder.append("职业：").append(PromptRuntimeUtil.nullableToken(role.getOccupation())).append("\n");
            builder.append("背景：").append(PromptRuntimeUtil.nullableToken(role.getBackgroundStory()));
        }
        return PromptRuntimeUtil.trimToNull(builder.toString());
    }

    /**
     * 归一化模型返回的候选列表，支持数组与分隔字符串。
     */
    private List<String> normalizeSuggestionList(Object rawValue) {
        List<String> result = new ArrayList<>();
        if (rawValue == null) {
            return result;
        }

        if (rawValue instanceof JSONArray) {
            JSONArray array = (JSONArray) rawValue;
            for (Object item : array) {
                addSuggestionCandidate(result, item == null ? null : String.valueOf(item));
            }
            return result;
        }

        if (rawValue instanceof List) {
            List<?> list = (List<?>) rawValue;
            for (Object item : list) {
                addSuggestionCandidate(result, item == null ? null : String.valueOf(item));
            }
            return result;
        }

        String plainText = PromptRuntimeUtil.trimToNull(String.valueOf(rawValue));
        if (!StringUtils.hasText(plainText)) {
            return result;
        }

        if (plainText.startsWith("[")) {
            try {
                JSONArray array = JSONArray.parseArray(plainText);
                for (Object item : array) {
                    addSuggestionCandidate(result, item == null ? null : String.valueOf(item));
                }
                return result;
            } catch (Exception ignored) {
                // fallback to delimiter split
            }
        }

        String[] parts = plainText.split("[\\n,，;；]");
        for (String part : parts) {
            addSuggestionCandidate(result, part);
        }
        return result;
    }

    /**
     * 增加单条候选并做去空白/限长处理。
     */
    private void addSuggestionCandidate(List<String> target, String rawSuggestion) {
        String value = PromptRuntimeUtil.trimToNull(rawSuggestion);
        if (!StringUtils.hasText(value)) {
            return;
        }
        value = value.replaceFirst("^[0-9一二三四五六七八九十]+[\\.|、\\)]\\s*", "");
        value = value.replaceAll("^[-•*]\\s*", "");
        if (value.length() > MAX_SUGGESTION_LENGTH) {
            value = value.substring(0, MAX_SUGGESTION_LENGTH);
        }
        target.add(value);
    }

    /**
     * 固定化候选数量，去重后不足 3 条时补齐兜底文案。
     */
    private List<String> ensureFixedSuggestions(List<String> source) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (source != null) {
            for (String item : source) {
                String value = PromptRuntimeUtil.trimToNull(item);
                if (value != null) {
                    unique.add(value);
                }
            }
        }
        List<String> result = new ArrayList<>(unique);
        addSuggestionCandidate(result, FALLBACK_SUGGESTION_1);
        addSuggestionCandidate(result, FALLBACK_SUGGESTION_2);
        addSuggestionCandidate(result, FALLBACK_SUGGESTION_3);

        LinkedHashSet<String> distinct = new LinkedHashSet<>(result);
        result = new ArrayList<>(distinct);
        if (result.size() > FIXED_SUGGESTION_COUNT) {
            return new ArrayList<>(result.subList(0, FIXED_SUGGESTION_COUNT));
        }
        String[] fallbacks = {FALLBACK_SUGGESTION_1, FALLBACK_SUGGESTION_2, FALLBACK_SUGGESTION_3};
        for (String fallback : fallbacks) {
            if (result.size() >= FIXED_SUGGESTION_COUNT) {
                break;
            }
            if (!result.contains(fallback)) {
                result.add(fallback);
            }
        }
        while (result.size() < FIXED_SUGGESTION_COUNT) {
            result.add(FALLBACK_SUGGESTION_3);
        }
        return result;
    }

    /**
     * 从 tool call 结果结构中提取 suggestions，兼容 arguments/function.arguments/tool_calls。
     */
    private List<String> extractToolCallSuggestions(JSONObject modelJson) {
        List<String> result = new ArrayList<>();
        if (modelJson == null) {
            return result;
        }

        // case 1: top-level arguments
        collectSuggestionsFromArguments(result, modelJson.get("arguments"));
        if (!result.isEmpty()) {
            return result;
        }

        // case 2: tool_call.arguments or tool_call.function.arguments
        JSONObject toolCall = toJsonObject(modelJson.get("tool_call"));
        if (toolCall != null) {
            collectSuggestionsFromArguments(result, toolCall.get("arguments"));
            if (!result.isEmpty()) {
                return result;
            }
            JSONObject function = toJsonObject(toolCall.get("function"));
            if (function != null) {
                collectSuggestionsFromArguments(result, function.get("arguments"));
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        // case 3: tool_calls[].function.arguments
        Object toolCallsObj = modelJson.get("tool_calls");
        if (toolCallsObj instanceof JSONArray) {
            JSONArray toolCalls = (JSONArray) toolCallsObj;
            for (Object item : toolCalls) {
                JSONObject oneCall = toJsonObject(item);
                if (oneCall == null) {
                    continue;
                }
                collectSuggestionsFromArguments(result, oneCall.get("arguments"));
                if (!result.isEmpty()) {
                    return result;
                }
                JSONObject function = toJsonObject(oneCall.get("function"));
                if (function != null) {
                    collectSuggestionsFromArguments(result, function.get("arguments"));
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        }

        // case 4: function_call.arguments
        JSONObject functionCall = toJsonObject(modelJson.get("function_call"));
        if (functionCall != null) {
            collectSuggestionsFromArguments(result, functionCall.get("arguments"));
        }
        return result;
    }

    private void collectSuggestionsFromArguments(List<String> target, Object argumentsObj) {
        JSONObject args = toJsonObject(argumentsObj);
        if (args == null) {
            return;
        }
        target.addAll(normalizeSuggestionList(args.get("suggestions")));
    }

    private JSONObject toJsonObject(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        if (raw instanceof Map) {
            JSONObject json = new JSONObject();
            Map<?, ?> map = (Map<?, ?>) raw;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "null" : String.valueOf(entry.getKey());
                json.put(key, entry.getValue());
            }
            return json;
        }
        if (raw instanceof String) {
            String text = PromptRuntimeUtil.trimToNull((String) raw);
            if (!StringUtils.hasText(text)) {
                return null;
            }
            try {
                return JSONObject.parseObject(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private JSONObject parseMessageContentJson(String contentJson) {
        String raw = PromptRuntimeUtil.trimToNull(contentJson);
        if (!StringUtils.hasText(raw)) {
            return new JSONObject();
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(raw);
            return jsonObject == null ? new JSONObject() : jsonObject;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private VoiceSelection resolveVoiceSelection(String userId, Long requestedVoiceProfileId, String requestedVoiceId) {
        String directVoiceId = PromptRuntimeUtil.trimToNull(requestedVoiceId);
        if (StringUtils.hasText(directVoiceId)) {
            return new VoiceSelection(directVoiceId, null, "VOICE_ID_DIRECT");
        }

        Long voiceProfileId = requestedVoiceProfileId;
        if (voiceProfileId == null) {
            TsUserVoiceConfig userVoiceConfig = tsUserVoiceConfigMapper.selectByUserId(userId);
            if (userVoiceConfig != null) {
                voiceProfileId = userVoiceConfig.getSelectedVoiceProfileId();
            }
        }
        if (voiceProfileId == null) {
            return new VoiceSelection(null, null, "VOICE_MODEL_DEFAULT");
        }

        TsVoiceProfile voiceProfile = tsVoiceProfileMapper.selectActiveById(voiceProfileId);
        if (voiceProfile == null) {
            throw new JeecgBootException("音色不存在或已停用，请重新选择");
        }
        if (!StringUtils.hasText(voiceProfile.getProviderVoiceId())) {
            throw new JeecgBootException("当前音色未配置 providerVoiceId，无法进行语音合成");
        }
        return new VoiceSelection(voiceProfile.getProviderVoiceId().trim(), voiceProfile.getId(), "VOICE_PROFILE");
    }

    private String sanitizeTtsText(String sourceText) {
        String raw = PromptRuntimeUtil.trimToNull(sourceText);
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String sanitized = raw;
        String previous;
        do {
            previous = sanitized;
            sanitized = TTS_BRACKET_PATTERN.matcher(sanitized).replaceAll(" ");
        } while (!previous.equals(sanitized));
        sanitized = sanitized.replace('\u3000', ' ');
        sanitized = sanitized.replaceAll("\\s*\\n\\s*", "\n");
        sanitized = TTS_MULTI_SPACE_PATTERN.matcher(sanitized).replaceAll(" ");
        sanitized = sanitized.replaceAll(" ?([，。！？；：,.!?;:])", "$1");
        sanitized = sanitized.replaceAll("([（(])\\s+", "$1");
        sanitized = sanitized.replaceAll("\\s+([）)])", "$1");
        sanitized = sanitized.trim();
        return StringUtils.hasText(sanitized) ? sanitized : raw;
    }

    private static final class VoiceSelection {
        private final String voiceId;
        private final Long voiceProfileId;
        private final String matchSource;

        private VoiceSelection(String voiceId, Long voiceProfileId, String matchSource) {
            this.voiceId = voiceId;
            this.voiceProfileId = voiceProfileId;
            this.matchSource = matchSource;
        }

        private String getVoiceId() {
            return voiceId;
        }

        private Long getVoiceProfileId() {
            return voiceProfileId;
        }

        private String getMatchSource() {
            return matchSource;
        }
    }

    private void startPlainChatUsage(String invocationId,
                                     LoginUser user,
                                     Long sessionId,
                                     Long messageId,
                                     Date startedAt) {
        AiUsageStartRequest usage = new AiUsageStartRequest();
        usage.setInvocationId(invocationId);
        usage.setTraceId(invocationId);
        usage.setUserId(user == null ? null : user.getId());
        usage.setSourceType("chat");
        usage.setSceneCode("normal_chat");
        usage.setModality("text");
        usage.setOperationType("chat_completion");
        usage.setProvider("MINIMAX");
        usage.setSessionId(sessionId);
        usage.setMessageId(messageId);
        usage.setStartedAt(startedAt);
        aiUsageRecorderService.start(usage);
    }

    private void finishPlainChatUsage(String invocationId,
                                      MiniMaxChatResponseVo response,
                                      Date startedAt,
                                      RuntimeException error) {
        AiUsageFinishRequest usage = new AiUsageFinishRequest();
        usage.setInvocationId(invocationId);
        usage.setStatus(error == null ? "success" : "failed");
        usage.setModelName(response == null ? null : response.getModelName());
        usage.setFinishedAt(new Date());
        usage.setDurationMs(response != null && response.getDurationMs() != null
                ? response.getDurationMs()
                : Math.max(0L, System.currentTimeMillis() - startedAt.getTime()));
        usage.setErrorCode(error == null ? null : "LLM_CALL_FAILED");
        usage.setErrorMessage(error == null ? null : error.getMessage());
        usage.setUsageRawJson(response == null ? null : response.getUsageRawJson());
        List<AiUsageMetricValue> metrics = new ArrayList<>();
        metrics.add(AiUsageMetricValue.of("request_count", 1, "count", "total"));
        addUsageMetric(metrics, AiUsageMetricValue.of(
                "input_tokens",
                response == null ? null : response.getInputTokens(),
                "token",
                "input"
        ));
        addUsageMetric(metrics, AiUsageMetricValue.of(
                "output_tokens",
                response == null ? null : response.getOutputTokens(),
                "token",
                "output"
        ));
        addUsageMetric(metrics, AiUsageMetricValue.of(
                "total_tokens",
                response == null ? null : response.getTotalTokens(),
                "token",
                "total"
        ));
        usage.setMetrics(metrics);
        aiUsageRecorderService.finish(usage);
    }

    private void addUsageMetric(List<AiUsageMetricValue> metrics, AiUsageMetricValue metric) {
        if (metric != null) {
            metrics.add(metric);
        }
    }

    private void logPlainChatRequest(Long sessionId,
                                     TsChatAiReplyDto request,
                                     List<TsChatMessage> historyMessages,
                                     String userContent,
                                     String renderedPrompt) {
        tsAiLogCollector.markModel("MINIMAX", "MINIMAX_DEMO", null);
        tsAiLogCollector.appendStep("llm_request", "模型请求", "success", step -> {
            step.setProvider("MINIMAX");
            step.setModelName("MINIMAX_DEMO");
            step.setDeveloperPrompt(PROMPT_SYSTEM);
            step.setUserPrompt(userContent);
            step.setRenderedPrompt(renderedPrompt);
            JSONObject payload = new JSONObject();
            payload.put("sessionId", sessionId);
            payload.put("historyCount", request == null ? null : request.getHistoryCount());
            payload.put("generateVoice", request == null ? null : request.getGenerateVoice());
            payload.put("voiceProfileId", request == null ? null : request.getVoiceProfileId());
            payload.put("voiceId", request == null ? null : request.getVoiceId());
            payload.put("historyMessageSize", historyMessages == null ? 0 : historyMessages.size());
            payload.put("promptLength", renderedPrompt == null ? 0 : renderedPrompt.length());
            step.setRequestPayloadJson(tsAiLogCollector.toJsonString(payload));
        });
    }

    private void logPlainChatResponse(String assistantContent) {
        tsAiLogCollector.appendStep("llm_response", "模型返回", StringUtils.hasText(assistantContent) ? "success" : "failed", step -> {
            step.setProvider("MINIMAX");
            step.setModelName("MINIMAX_DEMO");
            step.setResponseRaw(PromptRuntimeUtil.trimToNull(assistantContent));
        });
    }

    private void logTtsRequest(TsChatTtsSynthesizeDto request, String voiceMatchSource) {
        tsAiLogCollector.appendStep("tts_request", "语音请求", "success", step -> {
            JSONObject payload = new JSONObject();
            payload.put("voiceId", request == null ? null : request.getVoiceId());
            payload.put("voiceProfileId", request == null ? null : request.getVoiceProfileId());
            payload.put("matchSource", voiceMatchSource);
            payload.put("speed", request == null ? null : request.getSpeed());
            payload.put("pitch", request == null ? null : request.getPitch());
            payload.put("volume", request == null ? null : request.getVolume());
            payload.put("textLength", request == null || request.getText() == null ? 0 : request.getText().length());
            step.setRequestPayloadJson(tsAiLogCollector.toJsonString(payload));
        });
    }

    private void logTtsResponse(TsChatTtsResultVo response) {
        String audioUrl = response == null ? null : response.getAudioUrl();
        tsAiLogCollector.appendStep("tts_response", "语音返回", StringUtils.hasText(audioUrl) ? "success" : "failed", step -> {
            step.setResponseRaw(tsAiLogCollector.toJsonString(response));
            step.setFinalOutputJson(PromptRuntimeUtil.trimToNull(audioUrl));
        });
    }
}
