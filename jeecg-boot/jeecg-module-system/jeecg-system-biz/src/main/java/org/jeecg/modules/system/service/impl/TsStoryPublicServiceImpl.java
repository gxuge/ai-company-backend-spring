package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.annotation.TsBehaviorTrack;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicActionDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicQueryDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicSaveDto;
import org.jeecg.modules.system.constant.TsWorkReviewConstants;
import org.jeecg.modules.system.entity.TsPublicAuditLog;
import org.jeecg.modules.system.entity.TsPublicChannel;
import org.jeecg.modules.system.entity.TsStory;
import org.jeecg.modules.system.entity.TsStoryPublic;
import org.jeecg.modules.system.mapper.TsPublicAuditLogMapper;
import org.jeecg.modules.system.mapper.TsPublicChannelMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.mapper.TsStoryPublicMapper;
import org.jeecg.modules.system.service.ITsStoryPublicService;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicTargetOptionVo;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 故事公开记录 Service 实现。
 */
@Service
public class TsStoryPublicServiceImpl extends ServiceImpl<TsStoryPublicMapper, TsStoryPublic>
        implements ITsStoryPublicService {

    @Resource
    private TsStoryMapper tsStoryMapper;
    @Resource
    private TsPublicChannelMapper tsPublicChannelMapper;
    @Resource
    private TsPublicAuditLogMapper tsPublicAuditLogMapper;

    @Override
    public Result<Page<TsStoryPublicVo>> pagePublics(LoginUser user, TsStoryPublicQueryDto request) {
        TsStoryPublicQueryDto dto = request == null ? new TsStoryPublicQueryDto() : request;
        Page<TsStoryPublicVo> page = new Page<>(normalizePageNo(dto.getPageNo()), normalizePageSize(dto.getPageSize()));
        return Result.OK(baseMapper.selectManagePage(page, dto));
    }

    @Override
    public Result<TsStoryPublicVo> getPublic(LoginUser user, Long id) {
        TsStoryPublicVo vo = baseMapper.selectManageDetail(id);
        if (vo == null) {
            throw new JeecgBootException("故事公开记录不存在");
        }
        return Result.OK(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsStoryPublicVo> addPublic(LoginUser user, TsStoryPublicSaveDto request) {
        request.applyCreateDefaults();
        requireSelectableStory(request.getStoryId(), request.getOwnerUserId());
        TsPublicChannel channel = requireChannel(request.getChannelCode(), "story");
        ensureUniqueStoryChannel(null, request.getStoryId(), channel.getChannelCode());
        Date now = new Date();
        TsStoryPublic entity = new TsStoryPublic();
        applySaveRequest(entity, request);
        entity.setStatus("draft");
        entity.setCreateBy(user.getId());
        entity.setUpdateBy(user.getId());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        this.save(entity);
        insertAudit("story", entity.getId(), null, "draft", "create", user.getId(), null);
        return getPublic(user, entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsStoryPublicVo> editPublic(LoginUser user, Long id, TsStoryPublicSaveDto request) {
        TsStoryPublic entity = requireRecord(id);
        requireSelectableStory(request.getStoryId(), request.getOwnerUserId());
        TsPublicChannel channel = requireChannel(request.getChannelCode(), "story");
        ensureUniqueStoryChannel(id, request.getStoryId(), channel.getChannelCode());
        applySaveRequest(entity, request);
        entity.setUpdateBy(user.getId());
        entity.setUpdateTime(new Date());
        this.updateById(entity);
        return getPublic(user, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deletePublic(LoginUser user, Long id) {
        TsStoryPublic entity = requireRecord(id);
        this.removeById(entity.getId());
        insertAudit("story", entity.getId(), entity.getStatus(), "deleted", "delete", user.getId(), null);
        return Result.OK("删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsStoryPublicVo> submitPublic(LoginUser user, TsStoryPublicActionDto request) {
        return changeStatus(user, request, "pending", "submit");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @TsBehaviorTrack(
            eventType = "publish",
            resourceType = "story",
            userIdExpression = "#result.result.ownerUserId",
            resourceIdExpression = "#result.result.storyId")
    public Result<TsStoryPublicVo> approvePublic(LoginUser user, TsStoryPublicActionDto request) {
        return changeStatus(user, request, "online", "approve");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsStoryPublicVo> rejectPublic(LoginUser user, TsStoryPublicActionDto request) {
        return changeStatus(user, request, "rejected", "reject");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @TsBehaviorTrack(
            eventType = "publish",
            resourceType = "story",
            userIdExpression = "#result.result.ownerUserId",
            resourceIdExpression = "#result.result.storyId")
    public Result<TsStoryPublicVo> onlinePublic(LoginUser user, TsStoryPublicActionDto request) {
        return changeStatus(user, request, "online", "online");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsStoryPublicVo> offlinePublic(LoginUser user, TsStoryPublicActionDto request) {
        return changeStatus(user, request, "offline", "offline");
    }

    @Override
    public Result<Page<TsStoryPublicTargetOptionVo>> pageStoryOptions(LoginUser user, String ownerUserId, String keyword, Integer pageNo, Integer pageSize) {
        String normalizedOwnerUserId = trimToNull(ownerUserId);
        Page<TsStoryPublicTargetOptionVo> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        if (!StringUtils.hasText(normalizedOwnerUserId)) {
            page.setRecords(new java.util.ArrayList<>());
            return Result.OK(page);
        }
        LambdaQueryWrapper<TsStory> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(TsStory::getId, TsStory::getUserId, TsStory::getTitle, TsStory::getUpdatedAt);
        wrapper.eq(TsStory::getUserId, normalizedOwnerUserId)
                .eq(TsStory::getIsDeleted, 0)
                .and(q -> q.isNull(TsStory::getStatus).or().ne(TsStory::getStatus, 9));
        String normalizedKeyword = trimToNull(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(q -> q.like(TsStory::getTitle, normalizedKeyword)
                    .or()
                    .like(TsStory::getStoryIntro, normalizedKeyword));
        }
        wrapper.orderByDesc(TsStory::getUpdatedAt).orderByDesc(TsStory::getId);
        Page<TsStory> sourcePage = tsStoryMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        page.setTotal(sourcePage.getTotal());
        page.setCurrent(sourcePage.getCurrent());
        page.setSize(sourcePage.getSize());
        java.util.List<TsStoryPublicTargetOptionVo> records = new java.util.ArrayList<>();
        if (sourcePage.getRecords() != null) {
            for (TsStory story : sourcePage.getRecords()) {
                TsStoryPublicTargetOptionVo option = new TsStoryPublicTargetOptionVo();
                option.setValue(story.getId());
                option.setOwnerUserId(story.getUserId());
                option.setLabel(buildStoryOptionLabel(story));
                records.add(option);
            }
        }
        page.setRecords(records);
        return Result.OK(page);
    }

    private Result<TsStoryPublicVo> changeStatus(LoginUser user, TsStoryPublicActionDto request, String status, String action) {
        TsStoryPublic entity = requireRecord(request.getId());
        String beforeStatus = entity.getStatus();
        entity.setStatus(status);
        entity.setUpdateBy(user.getId());
        entity.setUpdateTime(new Date());
        if ("online".equals(status)) {
            entity.setPublishedAt(new Date());
        }
        if ("offline".equals(status)) {
            entity.setOfflineAt(new Date());
        }
        if ("rejected".equals(status)) {
            entity.setRejectReason(trimToNull(request.getRemark()));
        }
        this.updateById(entity);
        insertAudit("story", entity.getId(), beforeStatus, status, action, user.getId(), request.getRemark());
        return getPublic(user, entity.getId());
    }

    private TsStoryPublic requireRecord(Long id) {
        TsStoryPublic entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("故事公开记录不存在");
        }
        requireSelectableStory(entity.getStoryId(), null);
        return entity;
    }

    private TsStory requireSelectableStory(Long storyId, String ownerUserId) {
        TsStory story = storyId == null ? null : tsStoryMapper.selectById(storyId);
        if (story == null || !Integer.valueOf(0).equals(story.getIsDeleted()) || (story.getStatus() != null && story.getStatus() == 9)) {
            throw new JeecgBootException("故事不存在或不可公开");
        }
        if (!TsWorkReviewConstants.APPROVED.equals(story.getReviewStatus())) {
            throw new JeecgBootException("故事当前版本尚未通过作品审核");
        }
        String normalizedOwnerUserId = trimToNull(ownerUserId);
        if (StringUtils.hasText(normalizedOwnerUserId) && !normalizedOwnerUserId.equals(story.getUserId())) {
            throw new JeecgBootException("所选故事与所属用户不匹配");
        }
        return story;
    }

    private TsPublicChannel requireChannel(String channelCode, String targetType) {
        LambdaQueryWrapper<TsPublicChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsPublicChannel::getChannelCode, trimToNull(channelCode))
                .eq(TsPublicChannel::getStatus, "enabled");
        TsPublicChannel channel = tsPublicChannelMapper.selectOne(wrapper);
        if (channel == null) {
            throw new JeecgBootException("公开渠道不存在或未启用");
        }
        String actualTarget = trimToNull(channel.getTargetType());
        if (!"both".equals(actualTarget) && !targetType.equals(actualTarget)) {
            throw new JeecgBootException("当前渠道不支持故事公开");
        }
        return channel;
    }

    private void ensureUniqueStoryChannel(Long id, Long storyId, String channelCode) {
        LambdaQueryWrapper<TsStoryPublic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsStoryPublic::getStoryId, storyId)
                .eq(TsStoryPublic::getChannelCode, trimToNull(channelCode))
                .ne(id != null, TsStoryPublic::getId, id);
        if (this.count(wrapper) > 0) {
            throw new JeecgBootException("该故事在当前渠道已存在公开记录");
        }
    }

    private void applySaveRequest(TsStoryPublic entity, TsStoryPublicSaveDto request) {
        entity.setStoryId(request.getStoryId());
        entity.setChannelCode(trimToNull(request.getChannelCode()));
        entity.setDisplayTitle(trimToNull(request.getDisplayTitle()));
        entity.setDisplaySubtitle(trimToNull(request.getDisplaySubtitle()));
        entity.setCoverImageUrl(trimToNull(request.getCoverImageUrl()));
        entity.setIntroText(trimToNull(request.getIntroText()));
        entity.setSortOrder(request.getSortOrder());
        entity.setExtJson(request.getExtJson());
    }

    private void insertAudit(String targetType, Long publicId, String beforeStatus, String afterStatus,
                             String actionType, String operateBy, String remark) {
        TsPublicAuditLog log = new TsPublicAuditLog();
        log.setTargetType(targetType);
        log.setPublicId(publicId);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setActionType(actionType);
        log.setOperateBy(operateBy);
        log.setRemark(trimToNull(remark));
        log.setOperateTime(new Date());
        tsPublicAuditLogMapper.insert(log);
    }

    private long normalizePageNo(Integer value) {
        return value == null || value < 1 ? 1L : value;
    }

    private long normalizePageSize(Integer value) {
        if (value == null || value < 1) {
            return 10L;
        }
        return Math.min(value, 100);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildStoryOptionLabel(TsStory story) {
        String title = trimToNull(story.getTitle());
        if (!StringUtils.hasText(title)) {
            title = "未命名故事";
        }
        return title + "（ID:" + story.getId() + "）";
    }
}
