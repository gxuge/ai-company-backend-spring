package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsRoleImageProfile;
import org.jeecg.modules.system.po.tsroleimageprofile.TsRoleImageProfileQueryPo;
public interface TsRoleImageProfileMapper extends BaseMapper<TsRoleImageProfile> {
    Page<TsRoleImageProfile> selectProfilePage(Page<TsRoleImageProfile> page,
                                               @Param("query") TsRoleImageProfileQueryPo query);
    TsRoleImageProfile selectVisibleById(@Param("id") Long id, @Param("userId") String userId);
    TsRoleImageProfile selectOwnedById(@Param("id") Long id, @Param("userId") String userId);
}