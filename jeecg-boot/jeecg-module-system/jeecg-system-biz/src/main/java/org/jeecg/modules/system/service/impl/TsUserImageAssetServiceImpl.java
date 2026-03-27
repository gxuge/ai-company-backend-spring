package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsUserImageAssetOwnershipAspect;
import org.jeecg.modules.aop.TsUserImageAssetOwnershipAspect.CheckTsUserImageAssetOwnership;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetQueryDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetSaveDto;
import org.jeecg.modules.system.entity.TsUserImageAsset;
import org.jeecg.modules.system.mapper.TsUserImageAssetMapper;
import org.jeecg.modules.system.po.tsuserimageasset.TsUserImageAssetQueryPo;
import org.jeecg.modules.system.po.tsuserimageasset.TsUserImageAssetSavePo;
import org.jeecg.modules.system.service.ITsUserImageAssetService;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVo;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class TsUserImageAssetServiceImpl extends ServiceImpl<TsUserImageAssetMapper, TsUserImageAsset>
        implements ITsUserImageAssetService {
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
}
