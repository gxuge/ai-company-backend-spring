package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsUserImageAsset;
import org.jeecg.modules.system.po.tsuserimageasset.TsUserImageAssetQueryPo;
public interface TsUserImageAssetMapper extends BaseMapper<TsUserImageAsset> {
    Page<TsUserImageAsset> selectAssetPage(Page<TsUserImageAsset> page, @Param("query") TsUserImageAssetQueryPo query);
    TsUserImageAsset selectOwned(@Param("id") Long id, @Param("userId") Long userId);
}
