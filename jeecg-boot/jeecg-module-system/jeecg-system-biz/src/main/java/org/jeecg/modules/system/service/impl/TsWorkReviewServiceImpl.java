package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.constant.TsWorkReviewConstants;
import org.jeecg.modules.system.dto.tsworkreview.TsWorkReviewActionDto;
import org.jeecg.modules.system.dto.tsworkreview.TsWorkReviewQueryDto;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.entity.TsStoryRoleRel;
import org.jeecg.modules.system.entity.TsWorkReview;
import org.jeecg.modules.system.entity.TsWorkReviewItem;
import org.jeecg.modules.system.entity.TsWorkReviewLog;
import org.jeecg.modules.system.event.TsWorkReviewSubmittedEvent;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.mapper.TsStoryRoleRelMapper;
import org.jeecg.modules.system.mapper.TsWorkReviewItemMapper;
import org.jeecg.modules.system.mapper.TsWorkReviewLogMapper;
import org.jeecg.modules.system.mapper.TsWorkReviewMapper;
import org.jeecg.modules.system.review.TsWorkAiReviewer;
import org.jeecg.modules.system.service.ITsWorkReviewService;
import org.jeecg.modules.system.vo.tsworkreview.TsWorkReviewVo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TsWorkReviewServiceImpl extends ServiceImpl<TsWorkReviewMapper, TsWorkReview>
        implements ITsWorkReviewService {
    @Resource
    private TsRoleMapper tsRoleMapper;
    @Resource
    private TsStoryMapper tsStoryMapper;
    @Resource
    private TsStoryRoleRelMapper tsStoryRoleRelMapper;
    @Resource
    private TsWorkReviewItemMapper tsWorkReviewItemMapper;
    @Resource
    private TsWorkReviewLogMapper tsWorkReviewLogMapper;
    @Resource
    private TsWorkAiReviewer tsWorkAiReviewer;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsWorkReview submitRole(Long roleId, Integer requestedPublic) {
        TsRole role = roleId == null ? null : tsRoleMapper.selectById(roleId);
        if (role == null || (role.getStatus() != null && role.getStatus() == 0)) {
            throw new JeecgBootException("角色不存在或不可提交审核");
        }
        int version = nextVersion(role.getContentVersion());
        int desiredPublic = normalizeFlag(requestedPublic, role.getDesiredPublic(), role.getIsPublic());
        obsoleteActiveReviews(TsWorkReviewConstants.WORK_ROLE, roleId);

        Map<String, Object> snapshot = buildRoleSnapshot(role, version);
        TsWorkReview review = createReview(
                TsWorkReviewConstants.WORK_ROLE, roleId, role.getUserId(), version, desiredPublic, snapshot);

        role.setContentVersion(version);
        role.setReviewStatus(TsWorkReviewConstants.PENDING_AI);
        role.setCurrentReviewId(review.getId());
        role.setDesiredPublic(desiredPublic);
        role.setIsPublic(0);
        role.setUpdatedAt(new Date());
        tsRoleMapper.updateById(role);

        insertRoleItems(review.getId(), role);
        applicationEventPublisher.publishEvent(new TsWorkReviewSubmittedEvent(review.getId()));
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsWorkReview submitStory(Long storyId, Integer requestedPublic) {
        TsStory story = storyId == null ? null : tsStoryMapper.selectById(storyId);
        if (story == null || Integer.valueOf(1).equals(story.getIsDeleted())
                || (story.getStatus() != null && story.getStatus() == 9)) {
            throw new JeecgBootException("故事不存在或不可提交审核");
        }
        int version = nextVersion(story.getContentVersion());
        int desiredPublic = normalizeFlag(requestedPublic, story.getDesiredPublic(), story.getIsPublic());
        obsoleteActiveReviews(TsWorkReviewConstants.WORK_STORY, storyId);

        List<TsStoryRoleRel> roleBindings = tsStoryRoleRelMapper.selectByStoryId(storyId);
        Map<String, Object> snapshot = buildStorySnapshot(story, roleBindings, version);
        TsWorkReview review = createReview(
                TsWorkReviewConstants.WORK_STORY, storyId, story.getUserId(), version, desiredPublic, snapshot);

        story.setContentVersion(version);
        story.setReviewStatus(TsWorkReviewConstants.PENDING_AI);
        story.setCurrentReviewId(review.getId());
        story.setDesiredPublic(desiredPublic);
        story.setIsPublic(0);
        story.setUpdatedAt(new Date());
        tsStoryMapper.updateById(story);

        insertStoryItems(review.getId(), story);
        applicationEventPublisher.publishEvent(new TsWorkReviewSubmittedEvent(review.getId()));
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runAiReview(Long reviewId) {
        TsWorkReview review = requireReview(reviewId);
        if (!TsWorkReviewConstants.PENDING_AI.equals(review.getStatus()) || !isCurrentVersion(review)) {
            return;
        }
        Date now = new Date();
        try {
            JSONObject result = tsWorkAiReviewer.review(review);
            String decision = normalizeUpper(result.getString("decision"));
            String riskLevel = normalizeUpper(result.getString("risk_level"));
            String reason = trimToNull(result.getString("reason"));
            if (!TsWorkReviewConstants.AI_PASS.equals(decision)
                    && !TsWorkReviewConstants.AI_MANUAL.equals(decision)
                    && !TsWorkReviewConstants.AI_BLOCK.equals(decision)) {
                throw new JeecgBootException("AI审核结论不受支持");
            }
            String beforeStatus = review.getStatus();
            String afterStatus = TsWorkReviewConstants.AI_BLOCK.equals(decision)
                    ? TsWorkReviewConstants.REJECTED : TsWorkReviewConstants.PENDING_ADMIN;
            review.setAiDecision(decision);
            review.setAiRiskLevel(riskLevel);
            review.setAiReason(reason);
            review.setAiResultJson(result.toJSONString());
            review.setAiReviewedAt(now);
            review.setStatus(afterStatus);
            review.setUpdatedAt(now);
            baseMapper.updateById(review);
            updateAggregateReviewStatus(review, afterStatus, false);
            insertLog(review.getId(),
                    TsWorkReviewConstants.AI_BLOCK.equals(decision) ? "AI_BLOCK" : "AI_PASS",
                    beforeStatus, afterStatus, "AI", null, reason);
        } catch (Exception ex) {
            review.setAiReason(limit(ex.getMessage(), 1000));
            review.setAiReviewedAt(now);
            review.setUpdatedAt(now);
            baseMapper.updateById(review);
            insertLog(review.getId(), "AI_ERROR", TsWorkReviewConstants.PENDING_AI,
                    TsWorkReviewConstants.PENDING_AI, "AI", null, review.getAiReason());
        }
    }

    @Override
    public Result<TsWorkReviewVo> getCurrent(LoginUser user, String workType, Long workId) {
        String normalizedType = requireWorkType(workType);
        TsWorkReview review = findCurrent(normalizedType, workId);
        if (review == null || user == null || !review.getOwnerUserId().equals(user.getId())) {
            throw new JeecgBootException("审核任务不存在或无权访问");
        }
        return Result.OK(toVo(review, true));
    }

    @Override
    public Result<Page<TsWorkReviewVo>> pageAdmin(TsWorkReviewQueryDto request) {
        TsWorkReviewQueryDto dto = request == null ? new TsWorkReviewQueryDto() : request;
        LambdaQueryWrapper<TsWorkReview> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getWorkType())) {
            wrapper.eq(TsWorkReview::getWorkType, requireWorkType(dto.getWorkType()));
        }
        if (StringUtils.hasText(dto.getStatus())) {
            wrapper.eq(TsWorkReview::getStatus, normalizeUpper(dto.getStatus()));
        }
        if (StringUtils.hasText(dto.getOwnerUserId())) {
            wrapper.eq(TsWorkReview::getOwnerUserId, dto.getOwnerUserId().trim());
        }
        wrapper.orderByDesc(TsWorkReview::getSubmittedAt).orderByDesc(TsWorkReview::getId);
        Page<TsWorkReview> page = baseMapper.selectPage(
                new Page<>(normalizePageNo(dto.getPageNo()), normalizePageSize(dto.getPageSize())), wrapper);
        Page<TsWorkReviewVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<TsWorkReviewVo> records = new ArrayList<>();
        for (TsWorkReview review : page.getRecords()) {
            records.add(toVo(review, false));
        }
        result.setRecords(records);
        return Result.OK(result);
    }

    @Override
    public Result<TsWorkReviewVo> getAdminDetail(Long id) {
        return Result.OK(toVo(requireReview(id), true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsWorkReviewVo> approve(LoginUser user, TsWorkReviewActionDto request) {
        TsWorkReview review = requireAdminPending(request.getId());
        String beforeStatus = review.getStatus();
        Date now = new Date();
        review.setStatus(TsWorkReviewConstants.APPROVED);
        review.setAdminReviewerId(user.getId());
        review.setAdminReason(trimToNull(request.getReason()));
        review.setAdminReviewedAt(now);
        review.setUpdatedAt(now);
        baseMapper.updateById(review);
        updateAggregateReviewStatus(review, TsWorkReviewConstants.APPROVED, true);
        insertLog(review.getId(), "ADMIN_APPROVE", beforeStatus, TsWorkReviewConstants.APPROVED,
                "ADMIN", user.getId(), review.getAdminReason());
        return Result.OK("审核通过", toVo(review, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsWorkReviewVo> reject(LoginUser user, TsWorkReviewActionDto request) {
        TsWorkReview review = requireAdminPending(request.getId());
        String reason = trimToNull(request.getReason());
        if (!StringUtils.hasText(reason)) {
            throw new JeecgBootException("驳回原因不能为空");
        }
        String beforeStatus = review.getStatus();
        Date now = new Date();
        review.setStatus(TsWorkReviewConstants.REJECTED);
        review.setAdminReviewerId(user.getId());
        review.setAdminReason(reason);
        review.setAdminReviewedAt(now);
        review.setUpdatedAt(now);
        baseMapper.updateById(review);
        updateAggregateReviewStatus(review, TsWorkReviewConstants.REJECTED, false);
        insertLog(review.getId(), "ADMIN_REJECT", beforeStatus, TsWorkReviewConstants.REJECTED,
                "ADMIN", user.getId(), reason);
        return Result.OK("已驳回", toVo(review, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsWorkReviewVo> retryAi(TsWorkReviewActionDto request) {
        TsWorkReview review = requireReview(request.getId());
        if (!TsWorkReviewConstants.PENDING_AI.equals(review.getStatus()) || !isCurrentVersion(review)) {
            throw new JeecgBootException("当前任务不可重试AI审核");
        }
        review.setAiDecision(null);
        review.setAiRiskLevel(null);
        review.setAiReason(null);
        review.setAiResultJson(null);
        review.setAiReviewedAt(null);
        review.setUpdatedAt(new Date());
        baseMapper.updateById(review);
        applicationEventPublisher.publishEvent(new TsWorkReviewSubmittedEvent(review.getId()));
        return Result.OK("已重新提交AI审核", toVo(review, true));
    }

    private TsWorkReview createReview(String workType, Long workId, String ownerUserId, int version,
                                      int requestedPublic, Map<String, Object> snapshot) {
        String snapshotJson = JSON.toJSONString(snapshot);
        Date now = new Date();
        TsWorkReview review = new TsWorkReview();
        review.setReviewNo("WR" + UUID.randomUUID().toString().replace("-", ""));
        review.setWorkType(workType);
        review.setWorkId(workId);
        review.setOwnerUserId(ownerUserId);
        review.setWorkVersion(version);
        review.setRequestedPublic(requestedPublic);
        review.setSnapshotJson(snapshotJson);
        review.setSnapshotHash(sha256(snapshotJson));
        review.setStatus(TsWorkReviewConstants.PENDING_AI);
        review.setSubmittedAt(now);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        baseMapper.insert(review);
        insertLog(review.getId(), "SUBMIT", null, TsWorkReviewConstants.PENDING_AI,
                "SYSTEM", ownerUserId, null);
        return review;
    }

    private Map<String, Object> buildRoleSnapshot(TsRole role, int version) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workType", TsWorkReviewConstants.WORK_ROLE);
        snapshot.put("workId", role.getId());
        snapshot.put("workVersion", version);
        snapshot.put("roleName", role.getRoleName());
        snapshot.put("roleSubtitle", role.getRoleSubtitle());
        snapshot.put("avatarUrl", role.getAvatarUrl());
        snapshot.put("coverUrl", role.getCoverUrl());
        snapshot.put("gender", role.getGender());
        snapshot.put("occupation", role.getOccupation());
        snapshot.put("greeting", role.getGreeting());
        snapshot.put("backgroundStory", role.getBackgroundStory());
        snapshot.put("dialoguePreview", role.getDialoguePreview());
        snapshot.put("dialogueLength", role.getDialogueLength());
        snapshot.put("toneTendency", role.getToneTendency());
        snapshot.put("interactionMode", role.getInteractionMode());
        snapshot.put("voiceName", role.getVoiceName());
        snapshot.put("extJson", role.getExtJson());
        return snapshot;
    }

    private Map<String, Object> buildStorySnapshot(TsStory story, List<TsStoryRoleRel> bindings, int version) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workType", TsWorkReviewConstants.WORK_STORY);
        snapshot.put("workId", story.getId());
        snapshot.put("workVersion", version);
        snapshot.put("storyCode", story.getStoryCode());
        snapshot.put("title", story.getTitle());
        snapshot.put("storyIntro", story.getStoryIntro());
        snapshot.put("storyMode", story.getStoryMode());
        snapshot.put("siteSetting", story.getSiteSetting());
        snapshot.put("storyBackground", story.getStoryBackground());
        snapshot.put("coverUrl", story.getCoverUrl());
        snapshot.put("sceneImageUrl", story.getSceneImageUrl());
        snapshot.put("sceneId", story.getSceneId());
        snapshot.put("sceneNameSnapshot", story.getSceneNameSnapshot());
        snapshot.put("plotOutline", story.getPlotOutline());
        List<Map<String, Object>> roleBindings = new ArrayList<>();
        if (bindings != null) {
            for (TsStoryRoleRel binding : bindings) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("roleId", binding.getRoleId());
                item.put("roleType", binding.getRoleType());
                item.put("sortNo", binding.getSortNo());
                item.put("isRequired", binding.getIsRequired());
                item.put("joinSource", binding.getJoinSource());
                roleBindings.add(item);
            }
        }
        snapshot.put("roleBindings", roleBindings);
        return snapshot;
    }

    private void insertRoleItems(Long reviewId, TsRole role) {
        insertTextItem(reviewId, "roleName", role.getRoleName());
        insertTextItem(reviewId, "roleSubtitle", role.getRoleSubtitle());
        insertTextItem(reviewId, "occupation", role.getOccupation());
        insertTextItem(reviewId, "greeting", role.getGreeting());
        insertTextItem(reviewId, "backgroundStory", role.getBackgroundStory());
        insertTextItem(reviewId, "dialoguePreview", role.getDialoguePreview());
        insertTextItem(reviewId, "extJson", role.getExtJson());
        insertImageItem(reviewId, "avatarUrl", role.getAvatarUrl());
        insertImageItem(reviewId, "coverUrl", role.getCoverUrl());
    }

    private void insertStoryItems(Long reviewId, TsStory story) {
        insertTextItem(reviewId, "title", story.getTitle());
        insertTextItem(reviewId, "storyIntro", story.getStoryIntro());
        insertTextItem(reviewId, "siteSetting", story.getSiteSetting());
        insertTextItem(reviewId, "storyBackground", story.getStoryBackground());
        insertTextItem(reviewId, "sceneNameSnapshot", story.getSceneNameSnapshot());
        insertTextItem(reviewId, "plotOutline", story.getPlotOutline());
        insertImageItem(reviewId, "coverUrl", story.getCoverUrl());
        insertImageItem(reviewId, "sceneImageUrl", story.getSceneImageUrl());
    }

    private void insertTextItem(Long reviewId, String fieldCode, String value) {
        insertItem(reviewId, "TEXT", fieldCode, trimToNull(value), null);
    }

    private void insertImageItem(Long reviewId, String fieldCode, String value) {
        insertItem(reviewId, "IMAGE", fieldCode, null, trimToNull(value));
    }

    private void insertItem(Long reviewId, String itemType, String fieldCode, String contentText, String assetUrl) {
        String content = contentText != null ? contentText : assetUrl;
        if (!StringUtils.hasText(content)) {
            return;
        }
        TsWorkReviewItem item = new TsWorkReviewItem();
        item.setReviewId(reviewId);
        item.setItemType(itemType);
        item.setFieldCode(fieldCode);
        item.setContentText(contentText);
        item.setAssetUrl(assetUrl);
        item.setContentHash(sha256(content));
        item.setCreatedAt(new Date());
        tsWorkReviewItemMapper.insert(item);
    }

    private void obsoleteActiveReviews(String workType, Long workId) {
        LambdaQueryWrapper<TsWorkReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsWorkReview::getWorkType, workType)
                .eq(TsWorkReview::getWorkId, workId)
                .in(TsWorkReview::getStatus,
                        TsWorkReviewConstants.PENDING_AI, TsWorkReviewConstants.PENDING_ADMIN);
        List<TsWorkReview> reviews = baseMapper.selectList(wrapper);
        for (TsWorkReview review : reviews) {
            String beforeStatus = review.getStatus();
            review.setStatus(TsWorkReviewConstants.OBSOLETE);
            review.setUpdatedAt(new Date());
            baseMapper.updateById(review);
            insertLog(review.getId(), "OBSOLETE", beforeStatus, TsWorkReviewConstants.OBSOLETE,
                    "SYSTEM", null, "作品内容已更新");
        }
    }

    private void updateAggregateReviewStatus(TsWorkReview review, String status, boolean applyRequestedPublic) {
        if (!isCurrentVersion(review)) {
            return;
        }
        if (TsWorkReviewConstants.WORK_ROLE.equals(review.getWorkType())) {
            TsRole role = tsRoleMapper.selectById(review.getWorkId());
            role.setReviewStatus(status);
            role.setIsPublic(applyRequestedPublic ? review.getRequestedPublic() : 0);
            role.setUpdatedAt(new Date());
            tsRoleMapper.updateById(role);
            return;
        }
        TsStory story = tsStoryMapper.selectById(review.getWorkId());
        story.setReviewStatus(status);
        story.setIsPublic(applyRequestedPublic ? review.getRequestedPublic() : 0);
        story.setUpdatedAt(new Date());
        tsStoryMapper.updateById(story);
    }

    private boolean isCurrentVersion(TsWorkReview review) {
        if (TsWorkReviewConstants.WORK_ROLE.equals(review.getWorkType())) {
            TsRole role = tsRoleMapper.selectById(review.getWorkId());
            return role != null && review.getId().equals(role.getCurrentReviewId())
                    && review.getWorkVersion().equals(role.getContentVersion());
        }
        TsStory story = tsStoryMapper.selectById(review.getWorkId());
        return story != null && review.getId().equals(story.getCurrentReviewId())
                && review.getWorkVersion().equals(story.getContentVersion());
    }

    private TsWorkReview findCurrent(String workType, Long workId) {
        LambdaQueryWrapper<TsWorkReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsWorkReview::getWorkType, workType)
                .eq(TsWorkReview::getWorkId, workId)
                .orderByDesc(TsWorkReview::getWorkVersion)
                .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    private TsWorkReview requireAdminPending(Long id) {
        TsWorkReview review = requireReview(id);
        if (!TsWorkReviewConstants.PENDING_ADMIN.equals(review.getStatus()) || !isCurrentVersion(review)) {
            throw new JeecgBootException("当前任务不可进行管理员审核");
        }
        return review;
    }

    private TsWorkReview requireReview(Long id) {
        TsWorkReview review = id == null ? null : baseMapper.selectById(id);
        if (review == null) {
            throw new JeecgBootException("作品审核任务不存在");
        }
        return review;
    }

    private TsWorkReviewVo toVo(TsWorkReview review, boolean detail) {
        TsWorkReviewVo vo = new TsWorkReviewVo();
        vo.setId(review.getId());
        vo.setReviewNo(review.getReviewNo());
        vo.setWorkType(review.getWorkType());
        vo.setWorkId(review.getWorkId());
        vo.setWorkTitle(extractWorkTitle(review));
        vo.setOwnerUserId(review.getOwnerUserId());
        vo.setWorkVersion(review.getWorkVersion());
        vo.setRequestedPublic(review.getRequestedPublic());
        vo.setSnapshotHash(review.getSnapshotHash());
        vo.setStatus(review.getStatus());
        vo.setAiDecision(review.getAiDecision());
        vo.setAiRiskLevel(review.getAiRiskLevel());
        vo.setAiReason(review.getAiReason());
        vo.setAiReviewedAt(review.getAiReviewedAt());
        vo.setAdminReviewerId(review.getAdminReviewerId());
        vo.setAdminReason(review.getAdminReason());
        vo.setAdminReviewedAt(review.getAdminReviewedAt());
        vo.setSubmittedAt(review.getSubmittedAt());
        if (detail) {
            vo.setSnapshotJson(review.getSnapshotJson());
            vo.setAiResultJson(review.getAiResultJson());
            vo.setItems(tsWorkReviewItemMapper.selectList(new LambdaQueryWrapper<TsWorkReviewItem>()
                    .eq(TsWorkReviewItem::getReviewId, review.getId()).orderByAsc(TsWorkReviewItem::getId)));
            vo.setLogs(tsWorkReviewLogMapper.selectList(new LambdaQueryWrapper<TsWorkReviewLog>()
                    .eq(TsWorkReviewLog::getReviewId, review.getId()).orderByAsc(TsWorkReviewLog::getId)));
        }
        return vo;
    }

    private String extractWorkTitle(TsWorkReview review) {
        try {
            JSONObject snapshot = JSON.parseObject(review.getSnapshotJson());
            return TsWorkReviewConstants.WORK_ROLE.equals(review.getWorkType())
                    ? snapshot.getString("roleName") : snapshot.getString("title");
        } catch (Exception ignored) {
            return null;
        }
    }

    private void insertLog(Long reviewId, String actionType, String beforeStatus, String afterStatus,
                           String operatorType, String operatorId, String reason) {
        TsWorkReviewLog log = new TsWorkReviewLog();
        log.setReviewId(reviewId);
        log.setActionType(actionType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setReason(limit(reason, 1000));
        log.setCreatedAt(new Date());
        tsWorkReviewLogMapper.insert(log);
    }

    private int nextVersion(Integer currentVersion) {
        return currentVersion == null || currentVersion < 1 ? 1 : currentVersion + 1;
    }

    private int normalizeFlag(Integer... values) {
        if (values != null) {
            for (Integer value : values) {
                if (value != null) {
                    return value == 1 ? 1 : 0;
                }
            }
        }
        return 0;
    }

    private String requireWorkType(String workType) {
        String normalized = normalizeUpper(workType);
        if (!TsWorkReviewConstants.WORK_ROLE.equals(normalized)
                && !TsWorkReviewConstants.WORK_STORY.equals(normalized)) {
            throw new JeecgBootException("作品类型仅支持ROLE或STORY");
        }
        return normalized;
    }

    private long normalizePageNo(Integer value) {
        return value == null || value < 1 ? 1L : value;
    }

    private long normalizePageSize(Integer value) {
        return value == null || value < 1 ? 10L : Math.min(value, 100);
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new JeecgBootException("作品快照摘要生成失败");
        }
    }
}
