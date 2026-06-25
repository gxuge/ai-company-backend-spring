package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspublicchannel.TsPublicChannelQueryDto;
import org.jeecg.modules.system.dto.tspublicchannel.TsPublicChannelSaveDto;
import org.jeecg.modules.system.entity.TsPublicChannel;
import org.jeecg.modules.system.mapper.TsPublicChannelMapper;
import org.jeecg.modules.system.service.ITsPublicChannelService;
import org.jeecg.modules.system.vo.tspublicchannel.TsPublicChannelOptionVo;
import org.jeecg.modules.system.vo.tspublicchannel.TsPublicChannelVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 公开渠道 Service 实现。
 */
@Service
public class TsPublicChannelServiceImpl extends ServiceImpl<TsPublicChannelMapper, TsPublicChannel>
        implements ITsPublicChannelService {

    @Override
    public Result<Page<TsPublicChannelVo>> pageChannels(LoginUser user, TsPublicChannelQueryDto request) {
        TsPublicChannelQueryDto dto = request == null ? new TsPublicChannelQueryDto() : request;
        long pageNo = normalizePageNo(dto.getPageNo());
        long pageSize = normalizePageSize(dto.getPageSize());
        String keyword = trimToNull(dto.getKeyword());
        String targetType = trimToNull(dto.getTargetType());
        String status = trimToNull(dto.getStatus());
        LambdaQueryWrapper<TsPublicChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), TsPublicChannel::getChannelName, keyword)
                .eq(StringUtils.hasText(targetType), TsPublicChannel::getTargetType, targetType)
                .eq(StringUtils.hasText(status), TsPublicChannel::getStatus, status)
                .orderByAsc(TsPublicChannel::getSortOrder)
                .orderByDesc(TsPublicChannel::getUpdateTime)
                .orderByDesc(TsPublicChannel::getId);
        Page<TsPublicChannel> page = new Page<>(pageNo, pageSize);
        Page<TsPublicChannel> pageData = this.page(page, wrapper);
        return Result.OK(toVoPage(pageData));
    }

    @Override
    public Result<TsPublicChannelVo> getChannel(LoginUser user, Long id) {
        TsPublicChannel entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("公开渠道不存在");
        }
        return Result.OK(toVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsPublicChannelVo> addChannel(LoginUser user, TsPublicChannelSaveDto request) {
        request.applyCreateDefaults();
        ensureChannelCodeUnique(null, request.getChannelCode());
        Date now = new Date();
        TsPublicChannel entity = new TsPublicChannel();
        applySaveRequest(entity, request);
        entity.setCreateBy(user == null ? null : user.getId());
        entity.setUpdateBy(user == null ? null : user.getId());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        this.save(entity);
        return Result.OK("创建成功", toVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsPublicChannelVo> editChannel(LoginUser user, Long id, TsPublicChannelSaveDto request) {
        TsPublicChannel entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("公开渠道不存在");
        }
        ensureChannelCodeUnique(id, request.getChannelCode());
        applySaveRequest(entity, request);
        entity.setUpdateBy(user == null ? null : user.getId());
        entity.setUpdateTime(new Date());
        this.updateById(entity);
        return Result.OK("更新成功", toVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteChannel(LoginUser user, Long id) {
        TsPublicChannel entity = this.getById(id);
        if (entity == null) {
            throw new JeecgBootException("公开渠道不存在");
        }
        this.removeById(id);
        return Result.OK("删除成功");
    }

    @Override
    public Result<List<TsPublicChannelOptionVo>> listChannelOptions(LoginUser user, String targetType) {
        String normalizedTargetType = trimToNull(targetType);
        LambdaQueryWrapper<TsPublicChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsPublicChannel::getStatus, "enabled");
        if (StringUtils.hasText(normalizedTargetType)) {
            wrapper.and(q -> q.eq(TsPublicChannel::getTargetType, normalizedTargetType)
                    .or()
                    .eq(TsPublicChannel::getTargetType, "both"));
        }
        wrapper.orderByAsc(TsPublicChannel::getSortOrder)
                .orderByDesc(TsPublicChannel::getUpdateTime)
                .orderByDesc(TsPublicChannel::getId);
        List<TsPublicChannel> entities = this.list(wrapper);
        List<TsPublicChannelOptionVo> options = new ArrayList<>();
        if (entities != null) {
            for (TsPublicChannel entity : entities) {
                TsPublicChannelOptionVo option = new TsPublicChannelOptionVo();
                option.setLabel(entity.getChannelName());
                option.setValue(entity.getChannelCode());
                option.setImageUrl(entity.getChannelImageUrl());
                option.setTargetType(entity.getTargetType());
                options.add(option);
            }
        }
        return Result.OK(options);
    }

    private void ensureChannelCodeUnique(Long id, String channelCode) {
        String value = trimToNull(channelCode);
        if (!StringUtils.hasText(value)) {
            throw new JeecgBootException("channelCode不能为空");
        }
        LambdaQueryWrapper<TsPublicChannel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TsPublicChannel::getChannelCode, value);
        wrapper.ne(id != null, TsPublicChannel::getId, id);
        if (this.count(wrapper) > 0) {
            throw new JeecgBootException("channelCode已存在");
        }
    }

    private void applySaveRequest(TsPublicChannel entity, TsPublicChannelSaveDto request) {
        entity.setChannelCode(trimToNull(request.getChannelCode()));
        entity.setChannelName(trimToNull(request.getChannelName()));
        entity.setChannelImageUrl(trimToNull(request.getChannelImageUrl()));
        entity.setTargetType(trimToNull(request.getTargetType()));
        entity.setStatus(trimToNull(request.getStatus()));
        entity.setSortOrder(request.getSortOrder());
        entity.setRemark(trimToNull(request.getRemark()));
    }

    private Page<TsPublicChannelVo> toVoPage(Page<TsPublicChannel> source) {
        Page<TsPublicChannelVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsPublicChannelVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsPublicChannel item : source.getRecords()) {
                records.add(toVo(item));
            }
        }
        target.setRecords(records);
        return target;
    }

    private TsPublicChannelVo toVo(TsPublicChannel entity) {
        TsPublicChannelVo vo = new TsPublicChannelVo();
        vo.setId(entity.getId());
        vo.setChannelCode(entity.getChannelCode());
        vo.setChannelName(entity.getChannelName());
        vo.setChannelImageUrl(entity.getChannelImageUrl());
        vo.setTargetType(entity.getTargetType());
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
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
}
