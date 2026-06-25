package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicBrowseQueryDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicQueryDto;
import org.jeecg.modules.system.entity.TsRolePublic;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicBrowseVo;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicVo;

/**
 * 角色公开记录 Mapper。
 */
public interface TsRolePublicMapper extends BaseMapper<TsRolePublic> {
    Page<TsRolePublicVo> selectManagePage(Page<TsRolePublicVo> page,
                                          @Param("query") TsRolePublicQueryDto query);

    TsRolePublicVo selectManageDetail(@Param("id") Long id);

    Page<TsRolePublicBrowseVo> selectPublicBrowsePage(Page<TsRolePublicBrowseVo> page,
                                                      @Param("query") TsRolePublicBrowseQueryDto query);

    TsRolePublicBrowseVo selectPublicBrowseDetail(@Param("id") Long id,
                                                  @Param("publicId") Long publicId,
                                                  @Param("channelCode") String channelCode);
}
