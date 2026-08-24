package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsad.TsAdEventReportDto;
import org.jeecg.modules.system.entity.TsAdContent;
import org.jeecg.modules.system.entity.TsAdEvent;
import org.jeecg.modules.system.entity.TsAdSlot;
import org.jeecg.modules.system.enums.tsad.TsAdErrorCode;
import org.jeecg.modules.system.exception.tsad.TsAdBizException;
import org.jeecg.modules.system.mapper.TsAdContentMapper;
import org.jeecg.modules.system.mapper.TsAdQueryMapper;
import org.jeecg.modules.system.mapper.TsAdSlotMapper;
import org.jeecg.modules.system.po.tsad.TsAdDeliveryCandidatePo;
import org.jeecg.modules.system.service.ITsAdDeliveryService;
import org.jeecg.modules.system.service.ITsMemberService;
import org.jeecg.modules.system.util.tsad.TsAdConstants;
import org.jeecg.modules.system.util.tsad.TsAdRuleUtils;
import org.jeecg.modules.system.vo.tsad.TsAdDeliveryItemVo;
import org.jeecg.modules.system.vo.tsad.TsAdSlotDeliveryVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberCurrentVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** 前端广告投放服务实现。 */
@Service
public class TsAdDeliveryServiceImpl implements ITsAdDeliveryService {
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_]{2,64}");
    private static final long MAX_EVENT_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private static final long MAX_EVENT_FUTURE_MILLIS = 5L * 60 * 1000;

    private final TsAdQueryMapper queryMapper;
    private final TsAdContentMapper contentMapper;
    private final TsAdSlotMapper slotMapper;
    private final ITsMemberService memberService;
    private final ObjectMapper objectMapper;

    /** 注入投放查询、内容、广告位、会员与JSON组件。 */
    public TsAdDeliveryServiceImpl(
            TsAdQueryMapper queryMapper,
            TsAdContentMapper contentMapper,
            TsAdSlotMapper slotMapper,
            ITsMemberService memberService,
            ObjectMapper objectMapper) {
        this.queryMapper = queryMapper;
        this.contentMapper = contentMapper;
        this.slotMapper = slotMapper;
        this.memberService = memberService;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public List<TsAdSlotDeliveryVo> deliver(
            List<String> slotCodes, String platform, LoginUser loginUser) {
        List<String> normalizedSlotCodes = normalizeSlotCodes(slotCodes);
        String normalizedPlatform = normalizePlatform(platform);
        String memberLevel = resolveMemberLevel(loginUser);
        List<TsAdDeliveryCandidatePo> candidates =
                queryMapper.selectDeliveryCandidates(normalizedSlotCodes, new Date());
        Map<String, TsAdSlotDeliveryVo> grouped = new LinkedHashMap<>();
        Map<String, Integer> acceptedCounts = new LinkedHashMap<>();
        for (TsAdDeliveryCandidatePo candidate : candidates) {
            if (!matchesRule(candidate, normalizedPlatform, loginUser, memberLevel)) {
                continue;
            }
            int accepted = acceptedCounts.getOrDefault(candidate.getSlotCode(), 0);
            int maxItems = candidate.getMaxItems() == null ? 1 : candidate.getMaxItems();
            if (accepted >= maxItems) {
                continue;
            }
            TsAdSlotDeliveryVo slot = grouped.computeIfAbsent(
                    candidate.getSlotCode(), ignored -> toSlot(candidate));
            slot.getContents().add(toItem(candidate));
            acceptedCounts.put(candidate.getSlotCode(), accepted + 1);
        }
        return new ArrayList<>(grouped.values());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reportEvent(TsAdEventReportDto request, LoginUser loginUser) {
        TsAdContent content = contentMapper.selectById(request.getContentId());
        if (content == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_CONTENT_NOT_FOUND, "广告内容不存在");
        }
        String slotCode = normalizeSlotCode(request.getSlotCode());
        TsAdSlot slot = slotMapper.selectOne(
                new LambdaQueryWrapper<TsAdSlot>()
                        .eq(TsAdSlot::getId, content.getSlotId())
                        .eq(TsAdSlot::getSlotCode, slotCode)
                        .last("LIMIT 1"));
        if (slot == null) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "广告内容与广告位不匹配");
        }
        String visitorId = text(request.getVisitorId());
        if (loginUser == null && visitorId == null) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "匿名事件必须提供visitorId");
        }
        if (visitorId != null && visitorId.length() > 64) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "visitorId长度不能超过64");
        }
        Date now = new Date();
        Date occurredAt = request.getOccurredAt() == null ? now : request.getOccurredAt();
        long delta = occurredAt.getTime() - now.getTime();
        if (delta > MAX_EVENT_FUTURE_MILLIS || delta < -MAX_EVENT_AGE_MILLIS) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "事件时间超出允许范围");
        }
        TsAdEvent event = new TsAdEvent()
                .setEventId(normalizeEventId(request.getEventId()))
                .setContentId(content.getId())
                .setSlotCode(slotCode)
                .setEventType(normalizeEventType(request.getEventType()))
                .setUserId(loginUser == null ? null : loginUser.getId())
                .setVisitorId(visitorId)
                .setPlatform(normalizePlatform(request.getPlatform()))
                .setOccurredAt(occurredAt)
                .setCreatedAt(now);
        return queryMapper.insertEventIgnore(event) == 1;
    }

    /** 判断候选内容是否满足平台、登录、会员和指定用户规则。 */
    private boolean matchesRule(
            TsAdDeliveryCandidatePo candidate,
            String platform,
            LoginUser loginUser,
            String memberLevel) {
        List<String> platforms = TsAdRuleUtils.readList(
                objectMapper, candidate.getPlatformJson(), List.of("ALL"));
        List<String> memberLevels = TsAdRuleUtils.readList(
                objectMapper, candidate.getMemberLevelJson(), List.of("ALL"));
        if (!TsAdRuleUtils.allows(platforms, platform)
                || !TsAdRuleUtils.allows(memberLevels, memberLevel)) {
            return false;
        }
        String audienceType = StringUtils.hasText(candidate.getAudienceType())
                ? candidate.getAudienceType().trim().toUpperCase(Locale.ROOT) : "ALL";
        if ("ALL".equals(audienceType)) {
            return true;
        }
        if ("LOGIN".equals(audienceType)) {
            return loginUser != null;
        }
        if ("ANONYMOUS".equals(audienceType)) {
            return loginUser == null;
        }
        if (!"USER_LIST".equals(audienceType) || loginUser == null) {
            return false;
        }
        List<String> userIds = TsAdRuleUtils.readList(
                objectMapper, candidate.getUserIdJson(), List.of());
        return userIds.contains(loginUser.getId());
    }

    /** 获取登录用户会员等级，匿名用户固定为FREE。 */
    private String resolveMemberLevel(LoginUser loginUser) {
        if (loginUser == null) {
            return "FREE";
        }
        TsMemberCurrentVo membership = memberService.getCurrentMembership(loginUser);
        return membership == null || !StringUtils.hasText(membership.getPlanCode())
                ? "FREE" : membership.getPlanCode().trim().toUpperCase(Locale.ROOT);
    }

    /** 将候选查询模型转换为广告位响应。 */
    private TsAdSlotDeliveryVo toSlot(TsAdDeliveryCandidatePo candidate) {
        TsAdSlotDeliveryVo result = new TsAdSlotDeliveryVo();
        result.setSlotCode(candidate.getSlotCode());
        result.setSlotType(candidate.getSlotType());
        result.setWidth(candidate.getWidth());
        result.setHeight(candidate.getHeight());
        return result;
    }

    /** 将候选查询模型转换为前端内容响应。 */
    private TsAdDeliveryItemVo toItem(TsAdDeliveryCandidatePo candidate) {
        TsAdDeliveryItemVo result = new TsAdDeliveryItemVo();
        result.setId(candidate.getContentId());
        result.setContentCode(candidate.getContentCode());
        result.setTitle(candidate.getTitle());
        result.setSubtitle(candidate.getSubtitle());
        result.setSourceType(candidate.getSourceType());
        result.setMediaType(candidate.getMediaType());
        result.setMediaUrl(candidate.getMediaUrl());
        result.setPosterUrl(candidate.getPosterUrl());
        result.setCardType(candidate.getCardType());
        result.setPayloadJson(candidate.getPayloadJson());
        result.setImageUrl(candidate.getImageUrl());
        result.setActionType(candidate.getActionType());
        result.setActionPayload(candidate.getActionPayload());
        result.setLinkType(candidate.getLinkType());
        result.setLinkValue(candidate.getLinkValue());
        result.setExtJson(candidate.getExtJson());
        return result;
    }

    /** 归一化广告位编码列表并限制单次查询数量。 */
    private List<String> normalizeSlotCodes(List<String> slotCodes) {
        if (slotCodes == null || slotCodes.isEmpty()) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "至少提供一个广告位编码");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String slotCode : slotCodes) {
            normalized.add(normalizeSlotCode(slotCode));
        }
        if (normalized.size() > 20) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "单次最多查询20个广告位");
        }
        return new ArrayList<>(normalized);
    }

    /** 归一化单个广告位编码。 */
    private String normalizeSlotCode(String value) {
        String normalized = text(value);
        if (normalized == null) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "广告位编码不能为空");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "广告位编码格式不合法");
        }
        return normalized;
    }

    /** 归一化前端平台，不允许使用规则专用的ALL。 */
    private String normalizePlatform(String value) {
        String normalized = text(value);
        normalized = normalized == null ? "WEB" : normalized.toUpperCase(Locale.ROOT);
        if (!TsAdConstants.PLATFORMS.contains(normalized) || "ALL".equals(normalized)) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "平台仅支持WEB、IOS、ANDROID");
        }
        return normalized;
    }

    /** 归一化事件类型。 */
    private String normalizeEventType(String value) {
        String normalized = text(value);
        normalized = normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
        if (!TsAdConstants.EVENT_TYPES.contains(normalized)) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "事件类型不合法");
        }
        return normalized;
    }

    /** 归一化事件幂等ID。 */
    private String normalizeEventId(String value) {
        String normalized = text(value);
        if (normalized == null || normalized.length() > 64) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "eventId不能为空且长度不能超过64");
        }
        return normalized;
    }

    /** 去除可选文本首尾空格。 */
    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
