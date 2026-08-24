package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.dto.tsad.TsAdContentQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdContentSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdDeliveryRuleSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdSlotQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdSlotSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdStatsQueryDto;
import org.jeecg.modules.system.entity.TsAdContent;
import org.jeecg.modules.system.entity.TsAdDeliveryRule;
import org.jeecg.modules.system.entity.TsAdSlot;
import org.jeecg.modules.system.enums.tsad.TsAdErrorCode;
import org.jeecg.modules.system.exception.tsad.TsAdBizException;
import org.jeecg.modules.system.mapper.TsAdContentMapper;
import org.jeecg.modules.system.mapper.TsAdDeliveryRuleMapper;
import org.jeecg.modules.system.mapper.TsAdQueryMapper;
import org.jeecg.modules.system.mapper.TsAdSlotMapper;
import org.jeecg.modules.system.service.ITsAdAdminService;
import org.jeecg.modules.system.util.tsad.TsAdConstants;
import org.jeecg.modules.system.util.tsad.TsAdRuleUtils;
import org.jeecg.modules.system.vo.tsad.TsAdContentVo;
import org.jeecg.modules.system.vo.tsad.TsAdDeliveryRuleVo;
import org.jeecg.modules.system.vo.tsad.TsAdSlotVo;
import org.jeecg.modules.system.vo.tsad.TsAdStatsVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** 广告运营后台服务实现。 */
@Service
public class TsAdAdminServiceImpl implements ITsAdAdminService {
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_]{2,64}");

    private final TsAdSlotMapper slotMapper;
    private final TsAdContentMapper contentMapper;
    private final TsAdDeliveryRuleMapper ruleMapper;
    private final TsAdQueryMapper queryMapper;
    private final ObjectMapper objectMapper;

    /** 注入广告配置 Mapper 与JSON组件。 */
    public TsAdAdminServiceImpl(
            TsAdSlotMapper slotMapper,
            TsAdContentMapper contentMapper,
            TsAdDeliveryRuleMapper ruleMapper,
            TsAdQueryMapper queryMapper,
            ObjectMapper objectMapper) {
        this.slotMapper = slotMapper;
        this.contentMapper = contentMapper;
        this.ruleMapper = ruleMapper;
        this.queryMapper = queryMapper;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsAdSlotVo> pageSlots(TsAdSlotQueryDto request) {
        TsAdSlotQueryDto query = request == null ? new TsAdSlotQueryDto() : request;
        query.setKeyword(text(query.getKeyword()));
        query.setSlotType(optionalCode(
                query.getSlotType(), TsAdConstants.SLOT_TYPES, "广告位类型不合法"));
        query.setStatus(optionalCode(
                query.getStatus(), TsAdConstants.SLOT_STATUSES, "广告位状态不合法"));
        return queryMapper.selectSlotPage(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())), query);
    }

    /** {@inheritDoc} */
    @Override
    public TsAdSlotVo getSlot(Long id) {
        TsAdSlotVo detail = queryMapper.selectSlotDetail(id);
        if (detail == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_SLOT_NOT_FOUND, "广告位不存在");
        }
        return detail;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSlot(TsAdSlotSaveDto request, String operator) {
        String slotCode = requiredCode(
                request.getSlotCode(), null, "广告位编码格式不合法");
        assertSlotCodeUnique(slotCode, null);
        Date now = new Date();
        TsAdSlot slot = buildSlot(request)
                .setSlotCode(slotCode)
                .setCreatedBy(operator)
                .setUpdatedBy(operator)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setIsDeleted(0);
        slotMapper.insert(slot);
        return slot.getId();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSlot(TsAdSlotSaveDto request, String operator) {
        if (request.getId() == null || slotMapper.selectById(request.getId()) == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_SLOT_NOT_FOUND, "广告位不存在");
        }
        String slotCode = requiredCode(
                request.getSlotCode(), null, "广告位编码格式不合法");
        assertSlotCodeUnique(slotCode, request.getId());
        TsAdSlot slot = buildSlot(request)
                .setId(request.getId())
                .setSlotCode(slotCode)
                .setUpdatedBy(operator)
                .setUpdatedAt(new Date());
        if (slotMapper.updateById(slot) == 0) {
            throw new TsAdBizException(TsAdErrorCode.AD_SLOT_NOT_FOUND, "广告位不存在");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlot(Long id, String operator) {
        TsAdSlot slot = slotMapper.selectById(id);
        if (slot == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_SLOT_NOT_FOUND, "广告位不存在");
        }
        Long contentCount = contentMapper.selectCount(
                new LambdaQueryWrapper<TsAdContent>().eq(TsAdContent::getSlotId, id));
        if (contentCount != null && contentCount > 0) {
            throw new TsAdBizException(TsAdErrorCode.AD_SLOT_IN_USE, "广告位下仍有内容，不能删除");
        }
        slot.setUpdatedBy(operator).setUpdatedAt(new Date());
        slotMapper.updateById(slot);
        slotMapper.deleteById(id);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSlotStatus(Long id, String status, String operator) {
        TsAdSlot slot = slotMapper.selectById(id);
        if (slot == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_SLOT_NOT_FOUND, "广告位不存在");
        }
        slot.setStatus(requiredCode(
                        status, TsAdConstants.SLOT_STATUSES, "广告位状态不合法"))
                .setUpdatedBy(operator)
                .setUpdatedAt(new Date());
        slotMapper.updateById(slot);
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsAdContentVo> pageContents(TsAdContentQueryDto request) {
        TsAdContentQueryDto query =
                request == null ? new TsAdContentQueryDto() : request;
        query.setKeyword(text(query.getKeyword()));
        query.setStatus(optionalCode(
                query.getStatus(), TsAdConstants.CONTENT_STATUSES, "广告内容状态不合法"));
        return queryMapper.selectContentPage(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())), query);
    }

    /** {@inheritDoc} */
    @Override
    public TsAdContentVo getContent(Long id) {
        TsAdContentVo detail = queryMapper.selectContentDetail(id);
        if (detail == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_CONTENT_NOT_FOUND, "广告内容不存在");
        }
        return detail;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContent(TsAdContentSaveDto request, String operator) {
        requireSlot(request.getSlotId());
        String contentCode = StringUtils.hasText(request.getContentCode())
                ? requiredCode(request.getContentCode(), null, "广告内容编码格式不合法")
                : "AD_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        assertContentCodeUnique(contentCode, null);
        Date now = new Date();
        TsAdContent content = buildContent(request)
                .setContentCode(contentCode)
                .setStatus("DRAFT")
                .setCreatedBy(operator)
                .setUpdatedBy(operator)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setIsDeleted(0);
        contentMapper.insert(content);
        return content.getId();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContent(TsAdContentSaveDto request, String operator) {
        TsAdContent current = request.getId() == null
                ? null : contentMapper.selectById(request.getId());
        if (current == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_CONTENT_NOT_FOUND, "广告内容不存在");
        }
        requireSlot(request.getSlotId());
        String contentCode = StringUtils.hasText(request.getContentCode())
                ? requiredCode(request.getContentCode(), null, "广告内容编码格式不合法")
                : current.getContentCode();
        assertContentCodeUnique(contentCode, current.getId());
        Date now = new Date();
        TsAdContent content = buildContent(request)
                .setId(current.getId())
                .setContentCode(contentCode)
                .setStatus("DRAFT")
                .setOfflineAt("PUBLISHED".equals(current.getStatus()) ? now : current.getOfflineAt())
                .setUpdatedBy(operator)
                .setUpdatedAt(now);
        contentMapper.updateById(content);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContent(Long id, String operator) {
        TsAdContent content = contentMapper.selectById(id);
        if (content == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_CONTENT_NOT_FOUND, "广告内容不存在");
        }
        content.setUpdatedBy(operator).setUpdatedAt(new Date());
        contentMapper.updateById(content);
        ruleMapper.delete(new LambdaQueryWrapper<TsAdDeliveryRule>()
                .eq(TsAdDeliveryRule::getContentId, id));
        contentMapper.deleteById(id);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishContent(Long id, String operator) {
        TsAdContent content = requireContent(id);
        TsAdSlot slot = requireSlot(content.getSlotId());
        if (!"ENABLED".equals(slot.getStatus())) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_CONFIGURATION_INVALID, "广告位停用时不能发布内容");
        }
        Date now = new Date();
        content.setStatus("PUBLISHED")
                .setPublishAt(now)
                .setOfflineAt(null)
                .setUpdatedBy(operator)
                .setUpdatedAt(now);
        contentMapper.updateById(content);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineContent(Long id, String operator) {
        TsAdContent content = requireContent(id);
        Date now = new Date();
        content.setStatus("OFFLINE")
                .setOfflineAt(now)
                .setUpdatedBy(operator)
                .setUpdatedAt(now);
        contentMapper.updateById(content);
    }

    /** {@inheritDoc} */
    @Override
    public TsAdDeliveryRuleVo getDeliveryRule(Long contentId) {
        requireContent(contentId);
        TsAdDeliveryRule rule = ruleMapper.selectOne(
                new LambdaQueryWrapper<TsAdDeliveryRule>()
                        .eq(TsAdDeliveryRule::getContentId, contentId)
                        .last("LIMIT 1"));
        TsAdDeliveryRuleVo result = new TsAdDeliveryRuleVo();
        result.setContentId(contentId);
        if (rule == null) {
            result.setPlatforms(List.of("ALL"));
            result.setAudienceType("ALL");
            result.setMemberLevels(List.of("ALL"));
            return result;
        }
        result.setId(rule.getId());
        result.setAudienceType(rule.getAudienceType());
        result.setPlatforms(TsAdRuleUtils.readList(
                objectMapper, rule.getPlatformJson(), List.of("ALL")));
        result.setMemberLevels(TsAdRuleUtils.readList(
                objectMapper, rule.getMemberLevelJson(), List.of("ALL")));
        result.setUserIds(TsAdRuleUtils.readList(
                objectMapper, rule.getUserIdJson(), List.of()));
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDeliveryRule(TsAdDeliveryRuleSaveDto request, String operator) {
        requireContent(request.getContentId());
        List<String> platforms = normalizeList(
                request.getPlatforms(), TsAdConstants.PLATFORMS, "投放平台不合法");
        List<String> memberLevels = normalizeList(
                request.getMemberLevels(), TsAdConstants.MEMBER_LEVELS, "会员等级不合法");
        String audienceType = requiredCode(
                request.getAudienceType(), TsAdConstants.AUDIENCE_TYPES, "受众类型不合法");
        List<String> userIds = normalizeUserIds(request.getUserIds());
        if ("USER_LIST".equals(audienceType) && userIds.isEmpty()) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_CONFIGURATION_INVALID, "指定用户投放必须配置用户ID");
        }
        Date now = new Date();
        TsAdDeliveryRule current = ruleMapper.selectOne(
                new LambdaQueryWrapper<TsAdDeliveryRule>()
                        .eq(TsAdDeliveryRule::getContentId, request.getContentId())
                        .last("LIMIT 1"));
        TsAdDeliveryRule rule = new TsAdDeliveryRule()
                .setId(current == null ? null : current.getId())
                .setContentId(request.getContentId())
                .setPlatformJson(TsAdRuleUtils.writeList(objectMapper, platforms))
                .setAudienceType(audienceType)
                .setMemberLevelJson(TsAdRuleUtils.writeList(objectMapper, memberLevels))
                .setUserIdJson(userIds.isEmpty()
                        ? null : TsAdRuleUtils.writeList(objectMapper, userIds))
                .setUpdatedBy(operator)
                .setUpdatedAt(now);
        if (current == null) {
            rule.setCreatedBy(operator).setCreatedAt(now);
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
    }

    /** {@inheritDoc} */
    @Override
    public TsAdStatsVo getStats(TsAdStatsQueryDto request) {
        TsAdStatsQueryDto query = request == null ? new TsAdStatsQueryDto() : request;
        query.setSlotCode(text(query.getSlotCode()));
        if (query.getStartTime() != null
                && query.getEndTime() != null
                && query.getStartTime().after(query.getEndTime())) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "统计开始时间不能晚于结束时间");
        }
        return queryMapper.selectStats(query);
    }

    /** 构建并校验广告位实体。 */
    private TsAdSlot buildSlot(TsAdSlotSaveDto request) {
        return new TsAdSlot()
                .setSlotName(request.getSlotName().trim())
                .setSlotType(requiredCode(
                        request.getSlotType(), TsAdConstants.SLOT_TYPES, "广告位类型不合法"))
                .setWidth(request.getWidth())
                .setHeight(request.getHeight())
                .setMaxItems(request.getMaxItems() == null ? 1 : request.getMaxItems())
                .setStatus(StringUtils.hasText(request.getStatus())
                        ? requiredCode(request.getStatus(), TsAdConstants.SLOT_STATUSES,
                                "广告位状态不合法")
                        : "ENABLED")
                .setDescription(text(request.getDescription()));
    }

    /** 构建并校验广告内容实体。 */
    private TsAdContent buildContent(TsAdContentSaveDto request) {
        if (request.getStartTime() != null
                && request.getEndTime() != null
                && request.getStartTime().after(request.getEndTime())) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "投放开始时间不能晚于结束时间");
        }
        String sourceType = StringUtils.hasText(request.getSourceType())
                ? requiredCode(request.getSourceType(), TsAdConstants.SOURCE_TYPES, "素材来源不合法")
                : "SELF";
        String mediaType = StringUtils.hasText(request.getMediaType())
                ? requiredCode(request.getMediaType(), TsAdConstants.MEDIA_TYPES, "媒体类型不合法")
                : "IMAGE";
        String mediaUrl = text(StringUtils.hasText(request.getMediaUrl())
                ? request.getMediaUrl() : request.getImageUrl());
        String posterUrl = text(request.getPosterUrl());
        String cardType = StringUtils.hasText(request.getCardType())
                ? requiredCode(request.getCardType(), TsAdConstants.CARD_TYPES, "卡片类型不合法")
                : null;
        String payloadJson = validateCardPayload(mediaType, cardType, request.getPayloadJson());
        if ("CARD".equals(mediaType)) {
            mediaUrl = null;
            posterUrl = null;
        } else {
            if (mediaUrl == null) {
                throw new TsAdBizException(
                        TsAdErrorCode.AD_CONFIGURATION_INVALID, "图片或视频必须配置素材地址");
            }
            validateMediaUrl(sourceType, mediaUrl);
            cardType = null;
            payloadJson = null;
            if (!"VIDEO".equals(mediaType)) {
                posterUrl = null;
            }
        }
        String actionType = StringUtils.hasText(request.getActionType())
                ? requiredCode(request.getActionType(), TsAdConstants.ACTION_TYPES, "动作类型不合法")
                : (StringUtils.hasText(request.getLinkType())
                        ? requiredCode(request.getLinkType(), TsAdConstants.LINK_TYPES, "跳转类型不合法")
                        : "NONE");
        String actionPayload = text(StringUtils.hasText(request.getActionPayload())
                ? request.getActionPayload() : request.getLinkValue());
        if (!"NONE".equals(actionType) && actionPayload == null) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_CONFIGURATION_INVALID, "当前动作类型必须配置动作目标");
        }
        if ("URL".equals(actionType)) {
            validateHttpUrl(actionPayload, "外部链接必须是HTTP或HTTPS地址");
        }
        String extJson = validateExtJson(request.getExtJson());
        return new TsAdContent()
                .setSlotId(request.getSlotId())
                .setTitle(request.getTitle().trim())
                .setSubtitle(text(request.getSubtitle()))
                .setSourceType(sourceType)
                .setMediaType(mediaType)
                .setMediaUrl(mediaUrl)
                .setPosterUrl(posterUrl)
                .setCardType(cardType)
                .setPayloadJson(payloadJson)
                .setImageUrl(mediaUrl)
                .setActionType(actionType)
                .setActionPayload("NONE".equals(actionType) ? null : actionPayload)
                .setLinkType(actionType)
                .setLinkValue("NONE".equals(actionType) ? null : actionPayload)
                .setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .setStartTime(request.getStartTime())
                .setEndTime(request.getEndTime())
                .setExtJson(extJson);
    }

    /** 校验卡片内容必须是JSON对象。 */
    private String validateCardPayload(String mediaType, String cardType, String value) {
        String json = text(value);
        if ("CARD".equals(mediaType)) {
            if (cardType == null || json == null) {
                throw new TsAdBizException(
                        TsAdErrorCode.AD_CONFIGURATION_INVALID, "卡片必须配置卡片类型和内容JSON");
            }
            try {
                JsonNode node = objectMapper.readTree(json);
                if (node == null || !node.isObject()) {
                    throw new TsAdBizException(
                            TsAdErrorCode.AD_INVALID_ARGUMENT, "卡片内容必须是JSON对象");
                }
                return objectMapper.writeValueAsString(node);
            } catch (TsAdBizException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new TsAdBizException(
                        TsAdErrorCode.AD_INVALID_ARGUMENT, "卡片内容不是合法JSON");
            }
        }
        return null;
    }

    /** 校验素材地址；外部来源只允许HTTP或HTTPS。 */
    private void validateMediaUrl(String sourceType, String mediaUrl) {
        if ("EXTERNAL".equals(sourceType) || "AD_NETWORK".equals(sourceType)) {
            validateHttpUrl(mediaUrl, "外部素材必须是HTTP或HTTPS地址");
        }
    }

    /** 校验HTTP或HTTPS地址。 */
    private void validateHttpUrl(String value, String message) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new TsAdBizException(TsAdErrorCode.AD_INVALID_ARGUMENT, message);
            }
        } catch (IllegalArgumentException exception) {
            throw new TsAdBizException(TsAdErrorCode.AD_INVALID_ARGUMENT, message);
        }
    }

    /** 校验扩展JSON仅允许对象或数组。 */
    private String validateExtJson(String value) {
        String json = text(value);
        if (json == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || (!node.isObject() && !node.isArray())) {
                throw new TsAdBizException(
                        TsAdErrorCode.AD_INVALID_ARGUMENT, "扩展参数必须是JSON对象或数组");
            }
            return objectMapper.writeValueAsString(node);
        } catch (TsAdBizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "扩展参数不是合法JSON");
        }
    }

    /** 查询并校验广告位存在。 */
    private TsAdSlot requireSlot(Long id) {
        TsAdSlot slot = id == null ? null : slotMapper.selectById(id);
        if (slot == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_SLOT_NOT_FOUND, "广告位不存在");
        }
        return slot;
    }

    /** 查询并校验广告内容存在。 */
    private TsAdContent requireContent(Long id) {
        TsAdContent content = id == null ? null : contentMapper.selectById(id);
        if (content == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_CONTENT_NOT_FOUND, "广告内容不存在");
        }
        return content;
    }

    /** 校验广告位编码唯一。 */
    private void assertSlotCodeUnique(String code, Long excludedId) {
        LambdaQueryWrapper<TsAdSlot> wrapper =
                new LambdaQueryWrapper<TsAdSlot>().eq(TsAdSlot::getSlotCode, code);
        if (excludedId != null) {
            wrapper.ne(TsAdSlot::getId, excludedId);
        }
        if (slotMapper.selectCount(wrapper) > 0) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_CONFIGURATION_INVALID, "广告位编码已存在");
        }
    }

    /** 校验广告内容编码唯一。 */
    private void assertContentCodeUnique(String code, Long excludedId) {
        LambdaQueryWrapper<TsAdContent> wrapper =
                new LambdaQueryWrapper<TsAdContent>().eq(TsAdContent::getContentCode, code);
        if (excludedId != null) {
            wrapper.ne(TsAdContent::getId, excludedId);
        }
        if (contentMapper.selectCount(wrapper) > 0) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_CONFIGURATION_INVALID, "广告内容编码已存在");
        }
    }

    /** 归一化并校验规则列表。 */
    private List<String> normalizeList(
            List<String> values, Set<String> supported, String message) {
        if (values == null || values.isEmpty()) {
            throw new TsAdBizException(TsAdErrorCode.AD_INVALID_ARGUMENT, message);
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(requiredCode(value, supported, message));
        }
        if (normalized.contains("ALL") && normalized.size() > 1) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_CONFIGURATION_INVALID, "ALL不能与其他取值同时配置");
        }
        return new ArrayList<>(normalized);
    }

    /** 归一化指定用户ID列表。 */
    private List<String> normalizeUserIds(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String userId = text(value);
            if (userId != null) {
                if (userId.length() > 32) {
                    throw new TsAdBizException(
                            TsAdErrorCode.AD_INVALID_ARGUMENT, "用户ID长度不能超过32");
                }
                normalized.add(userId);
            }
        }
        if (normalized.size() > 500) {
            throw new TsAdBizException(
                    TsAdErrorCode.AD_INVALID_ARGUMENT, "单条规则最多配置500个用户");
        }
        return new ArrayList<>(normalized);
    }

    /** 归一化必填编码。 */
    private String requiredCode(String value, Set<String> supported, String message) {
        String normalized = text(value);
        if (normalized == null) {
            throw new TsAdBizException(TsAdErrorCode.AD_INVALID_ARGUMENT, message);
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if ((supported != null && !supported.contains(normalized))
                || (supported == null && !CODE_PATTERN.matcher(normalized).matches())) {
            throw new TsAdBizException(TsAdErrorCode.AD_INVALID_ARGUMENT, message);
        }
        return normalized;
    }

    /** 归一化可选编码。 */
    private String optionalCode(String value, Set<String> supported, String message) {
        return StringUtils.hasText(value) ? requiredCode(value, supported, message) : null;
    }

    /** 去除可选文本首尾空格。 */
    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 归一化页码。 */
    private int pageNo(Integer value) {
        return value == null ? 1 : Math.max(value, 1);
    }

    /** 归一化分页大小。 */
    private int pageSize(Integer value) {
        return value == null ? 10 : Math.min(Math.max(value, 1), 100);
    }
}
