package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.po.tsrole.TsRoleQueryPo;

import java.util.List;
public interface TsRoleMapper extends BaseMapper<TsRole> {
    Page<TsRole> selectRolePage(Page<TsRole> page, @Param("query") TsRoleQueryPo query);
    TsRole selectOwned(@Param("id") Long id, @Param("userId") String userId);
    List<Long> selectOwnedIds(@Param("roleIds") List<Long> roleIds, @Param("userId") String userId);
}