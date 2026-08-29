package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicActionDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicQueryDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicSaveDto;
import org.jeecg.modules.system.constant.TsWorkReviewConstants;
import org.jeecg.modules.system.entity.TsPublicAuditLog;
import org.jeecg.modules.system.entity.TsPublicChannel;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsRolePublic;
import org.jeecg.modules.system.mapper.TsPublicAuditLogMapper;
import org.jeecg.modules.system.mapper.TsPublicChannelMapper;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsRolePublicMapper;
import org.jeecg.modules.system.service.ITsRolePublicService;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicTargetOptionVo;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 角色公开记录 Service 实现。
 */
@Service
public class TsRolePublicServiceImpl extends ServiceImpl<TsRolePublicMapper, TsRolePublic>
        implements ITsRolePublicService {

    @Resource
    private TsRoleMapper tsRoleMapper;
    @Resource
    private TsPublicChannelMapper tsPublicChannelMapper;
    @Resource
    private TsPublicAuditLogMapper tsPublicAuditLogMapper;

    @Override
    public Result<Page<TsRolePublicVo>> pagePublics(LoginUser user, TsRolePublicQueryDto request) {
        TsRolePublicQueryDto dto = request == null ? new TsRolePublicQueryDto() : request;
        Page<TsRolePublicVo> page = new Page<>(normalizePageNo(dto.getPageNo()), normalizePageSize(dto.getPageSize()));
        return Result.OK(baseMapper.selectManagePage(page, dto));
    }

    @Override
    public Result<TsRolePublicVo> getPublic(LoginUser user, Long id) {
        TsRolePublicVo vo = baseMapper.selectManageDetail(id);
        if (vo == null) {
            throw new JeecgBootException("角色公开记录不存在");
        }
        return Result.OK(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRolePublicVo> addPublic(LoginUser user, TsRolePublicSaveDto request) {
        request.applyCreateDefaults();
        TsRole role = requireSelectableRole(request.getRoleId(), request.getOwnerUserId());
        TsPublicChannel channel = requireChannel(request.getChannelCode(), "role");
        ensureUniqueRoleChannel(null, request.getRoleId(), channel.getChannelCode());
        Date now = new Date();
        TsRolePublic entity = new TsRolePublic();
        applySaveRequest(entity, request);
        entity.setStatus("draft");
        entity.setCreateBy(user.getId());
        entity.setUpdateBy(user.getId());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        this.save(entity);
        insertAudit("role", entity.getId(), null, "draft", "create", user.getId(), null);
        return getPublic(user, entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRolePublicVo> editPublic(LoginUser user, Long id, TsRolePublicSaveDto request) {
        TsRolePublic entity = requireRecord(id);
        requireSelectableRole(request.getRoleId(), request.getOwnerUserId());
        TsPublicChannel channel = requireChannel(request.getChannelCode(), "role");
        ensureUniqueRoleChannel(id, request.getRoleId(), channel.getChannelCode());
        applySaveRequest(entity, request);
        entity.setUpdateBy(user.getId());
        entity.setUpdateTime(new Date());
        this.updateById(entity);
        return getPublic(user, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deletePublic(LoginUser user, Long id) {
        TsRolePublic entity = requireRecord(id);
        this.removeById(entity.getId());
        insertAudit("role", entity.getId(), entity.getStatus(), "deleted", "delete", user.getId(), null);
        return Result.OK("删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRolePublicVo> submitPublic(LoginUser user, TsRolePublicActionDto request) {
        return changeStatus(user, request, "pending", "submit");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRolePublicVo> approvePublic(LoginUser user, TsRolePublicActionDto request) {
        return changeStatus(user, request, "online", "approve");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRolePublicVo> rejectPublic(LoginUser user, TsRolePublicActionDto request) {
        return changeStatus(user, request, "rejected", "reject");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRolePublicVo> onlinePublic(LoginUser user, TsRolePublicActionDto request) {
        return changeStatus(user, request, "online", "online");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsRolePublicVo> offlinePublic(LoginUser user, TsRolePublicActionDto request) {
        return changeStatus(user, request, "offline", "offline");
    }

    @Override
    public Result<Page<TsRolePublicTargetOptionVo>> pageRoleOptions(LoginUser user, String ownerUserId, String keyword, Integer pageNo, Integer pageSize) {
        String normalizedOwnerUserId = trimToNull(ownerUserId);
        Page<TsRolePublicTargetOptionVo> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        if (!StringUtils.hasText(normalizedOwnerUserId)) {
            page.setRecords(new java.util.ArrayList<>());
            return Result.OK(page);
        }
        LambdaQueryWrapper<TsRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(TsRole::getId, TsRole::getUserId, TsRole::getRoleName, TsRole::getUpdatedAt);
        wrapper.eq(TsRole::getUserId, normalizedOwnerUserId)
                .and(q -> q.isNull(TsRole::getStatus).or().ne(TsRole::getStatus, 0));
        String normalizedKeyword = trimToNull(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(q -> q.like(TsRole::getRoleName, normalizedKeyword)
                    .or()
                    .like(TsRole::getRoleSubtitle, normalizedKeyword)
                    .or()
                    .like(TsRole::getBackgroundStory, normalizedKeyword)
                    .or()
                    .like(TsRole::getGreeting, normalizedKeyword));
        }
        wrapper.orderByDesc(TsRole::getUpdatedAt).orderByDesc(TsRole::getId);
        Page<TsRole> sourcePage = tsRoleMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        page.setTotal(sourcePage.getTotal());
        page.setCurrent(sourcePage.getCurrent());
        page.setSize(sourcePage.getSize());
        java.util.List<TsRolePublicTargetOptionVo> records = new java.util.ArrayList<>();
        if (sourcePage.getRecords() != null) {
            for (TsRole role : sourcePage.getRecords()) {
                TsRolePublicTargetOptionVo option = new TsRolePublicTargetOptionVo();
                option.setValue(role.getId());
                option.setOwnerUserId(role.getUserId());
                option.setLabel(buildRoleOptionLabel(role));
                records.add(option);
            }
        }
        page.setRecords(records);
        return Result.OK(page);
    }

    private Result<TsRolePublicVo> changeStatus(LoginUser user, TsRolePublicActionDto request, String status, String action) {
        TsRolePublic entity = requireRecord(request.getId());
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
        insertAudit("role", entity.getId(), beforeStatus, status, action, user.getId(), request.getRemark());
        return getPublic(user, entity.getId());
    }

    private TsRolePublic requireRecord(Long id) {
        TsRolePublic entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("角色公开记录不存在");
        }
        requireSelectableRole(entity.getRoleId(), null);
        return entity;
    }

    private TsRole requireSelectableRole(Long roleId, String ownerUserId) {
        TsRole role = roleId == null ? null : tsRoleMapper.selectById(roleId);
        if (role == null || (role.getStatus() != null && role.getStatus() == 0)) {
            throw new JeecgBootException("角色不存在或不可公开");
        }
        if (!TsWorkReviewConstants.APPROVED.equals(role.getReviewStatus())) {
            throw new JeecgBootException("角色当前版本尚未通过作品审核");
        }
        String normalizedOwnerUserId = trimToNull(ownerUserId);
        if (StringUtils.hasText(normalizedOwnerUserId) && !normalizedOwnerUserId.equals(role.getUserId())) {
            throw new JeecgBootException("所选角色与所属用户不匹配");
        }
        return role;
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
            throw new JeecgBootException("当前渠道不支持角色公开");
        }
        return channel;
    }

    private void ensureUniqueRoleChannel(Long id, Long roleId, String channelCode) {
        LambdaQueryWrapper<TsRolePublic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsRolePublic::getRoleId, roleId)
                .eq(TsRolePublic::getChannelCode, trimToNull(channelCode))
                .ne(id != null, TsRolePublic::getId, id);
        if (this.count(wrapper) > 0) {
            throw new JeecgBootException("该角色在当前渠道已存在公开记录");
        }
    }

    private void applySaveRequest(TsRolePublic entity, TsRolePublicSaveDto request) {
        entity.setRoleId(request.getRoleId());
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

    private String buildRoleOptionLabel(TsRole role) {
        String roleName = trimToNull(role.getRoleName());
        if (!StringUtils.hasText(roleName)) {
            roleName = "未命名角色";
        }
        return roleName + "（ID:" + role.getId() + "）";
    }
}
