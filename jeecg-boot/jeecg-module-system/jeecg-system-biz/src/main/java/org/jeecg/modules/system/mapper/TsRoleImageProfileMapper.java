package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsRoleImageProfile;
import org.jeecg.modules.system.po.tsroleimageprofile.TsRoleImageProfileQueryPo;
import org.jeecg.modules.system.vo.tsroleimageprofile.TsRoleImageProfilePublicVo;
public interface TsRoleImageProfileMapper extends BaseMapper<TsRoleImageProfile> {
    Page<TsRoleImageProfile> selectProfilePage(Page<TsRoleImageProfile> page,
                                               @Param("query") TsRoleImageProfileQueryPo query);
    Page<TsRoleImageProfilePublicVo> selectPublicProfilePage(Page<TsRoleImageProfilePublicVo> page,
                                                             @Param("keyword") String keyword,
                                                             @Param("styleName") String styleName,
                                                             @Param("sourceType") String sourceType);
    TsRoleImageProfile selectVisibleById(@Param("id") Long id, @Param("userId") String userId);
    TsRoleImageProfile selectOwnedById(@Param("id") Long id, @Param("userId") String userId);
}
