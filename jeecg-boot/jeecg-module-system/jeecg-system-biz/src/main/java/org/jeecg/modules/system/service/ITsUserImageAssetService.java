package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetQueryDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetSaveDto;
import org.jeecg.modules.system.entity.TsUserImageAsset;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVo;
public interface ITsUserImageAssetService extends IService<TsUserImageAsset> {
    Result<Page<TsUserImageAssetVo>> pageAssets(LoginUser user, TsUserImageAssetQueryDto request);
    Result<TsUserImageAssetVo> getAsset(LoginUser user, Long id);
    Result<TsUserImageAssetVo> addAsset(LoginUser user, TsUserImageAssetSaveDto request);
    Result<TsUserImageAssetVo> editAsset(LoginUser user, Long id, TsUserImageAssetSaveDto request);
    Result<?> deleteAsset(LoginUser user, Long id);
}
