package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsUserImageAssetOwnershipAspect;
import org.jeecg.modules.aop.TsUserImageAssetOwnershipAspect.CheckTsUserImageAssetOwnership;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetImportDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetQueryDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetSaveDto;
import org.jeecg.modules.system.entity.TsUserImageAsset;
import org.jeecg.modules.system.mapper.TsUserImageAssetMapper;
import org.jeecg.modules.system.po.tsuserimageasset.TsUserImageAssetQueryPo;
import org.jeecg.modules.system.po.tsuserimageasset.TsUserImageAssetSavePo;
import org.jeecg.modules.system.service.ITsUserImageAssetService;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVo;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVoConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import jakarta.annotation.Resource;
@Service
public class TsUserImageAssetServiceImpl extends ServiceImpl<TsUserImageAssetMapper, TsUserImageAsset>
        implements ITsUserImageAssetService {
    @Resource
    private IMiniMaxDemoService miniMaxDemoService;

    @Override
    public Result<Page<TsUserImageAssetVo>> pageAssets(LoginUser user, TsUserImageAssetQueryDto request) {
        Long userId = Long.valueOf(user.getId());
        TsUserImageAssetQueryPo queryPo = TsUserImageAssetQueryPo.fromRequest(userId, request);
        Page<TsUserImageAsset> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsUserImageAsset> pageData = baseMapper.selectAssetPage(page, queryPo);
        return Result.OK(TsUserImageAssetVoConverter.fromPage(pageData));
    }
    @Override
    @CheckTsUserImageAssetOwnership(message = "素材不存在或无权限访问")
    public Result<TsUserImageAssetVo> getAsset(LoginUser user, Long id) {
        TsUserImageAsset entity = TsUserImageAssetOwnershipAspect.ASSET_CONTEXT.get();
        return Result.OK(TsUserImageAssetVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserImageAssetVo> addAsset(LoginUser user, TsUserImageAssetSaveDto request) {
        Long userId = Long.valueOf(user.getId());

        request.applyCreateDefaults();
        TsUserImageAssetSavePo savePo = TsUserImageAssetSavePo.fromRequest(request);

        Date now = new Date();
        TsUserImageAsset entity = new TsUserImageAsset();
        savePo.applyTo(entity);
        entity.setUserId(userId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);

        return Result.OK("创建成功", TsUserImageAssetVoConverter.fromEntity(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserImageAssetVo> importAsset(LoginUser user, TsUserImageAssetImportDto request) {
        Long userId = Long.valueOf(user.getId());
        String sourceKey = trimToNull(request.getSourceKey());
        if (sourceKey != null) {
            TsUserImageAsset existing = baseMapper.selectBySourceKey(userId, sourceKey);
            if (existing != null) {
                return restoreOrReturnExisting(existing);
            }
        }

        String persistedUrl = miniMaxDemoService.persistGeneratedImage(request.getSourceImageUrl());
        Date now = new Date();
        TsUserImageAsset entity = new TsUserImageAsset();
        entity.setUserId(userId);
        entity.setFileUrl(persistedUrl);
        entity.setThumbnailUrl(persistedUrl);
        entity.setFileName(StringUtils.hasText(request.getFileName())
                ? request.getFileName().trim()
                : "ai-image-" + System.currentTimeMillis() + ".png");
        entity.setSourceType(StringUtils.hasText(request.getSourceType())
                ? request.getSourceType().trim()
                : "ai_generate");
        entity.setSourceKey(sourceKey);
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        try {
            this.save(entity);
        } catch (DuplicateKeyException duplicateKeyException) {
            TsUserImageAsset existing = baseMapper.selectBySourceKey(userId, sourceKey);
            if (existing != null) {
                return restoreOrReturnExisting(existing);
            }
            throw duplicateKeyException;
        }

        return buildImportResult(entity, false, "保存成功");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsUserImageAssetOwnership(message = "素材不存在或无权限修改")
    public Result<TsUserImageAssetVo> editAsset(LoginUser user, Long id, TsUserImageAssetSaveDto request) {
        TsUserImageAsset entity = TsUserImageAssetOwnershipAspect.ASSET_CONTEXT.get();

        TsUserImageAssetSavePo savePo = TsUserImageAssetSavePo.fromRequest(request);
        savePo.applyTo(entity);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);

        return Result.OK("更新成功", TsUserImageAssetVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsUserImageAssetOwnership(message = "素材不存在或无权限删除")
    public Result<?> deleteAsset(LoginUser user, Long id) {
        TsUserImageAsset entity = TsUserImageAssetOwnershipAspect.ASSET_CONTEXT.get();

        entity.setStatus(0);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        return Result.OK("删除成功");
    }

    private Result<TsUserImageAssetVo> restoreOrReturnExisting(TsUserImageAsset existing) {
        if (existing.getStatus() != null && existing.getStatus() != 0) {
            return buildImportResult(existing, true, "已保存到图库");
        }
        existing.setStatus(1);
        existing.setUpdatedAt(new Date());
        this.updateById(existing);
        return buildImportResult(existing, false, "保存成功");
    }

    private Result<TsUserImageAssetVo> buildImportResult(
            TsUserImageAsset entity,
            boolean alreadySaved,
            String message) {
        TsUserImageAssetVo vo = TsUserImageAssetVoConverter.fromEntity(entity);
        vo.setAlreadySaved(alreadySaved);
        return Result.OK(message, vo);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
