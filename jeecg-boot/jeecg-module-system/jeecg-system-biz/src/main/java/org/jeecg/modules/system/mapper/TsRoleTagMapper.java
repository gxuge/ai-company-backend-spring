package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.system.entity.TsRoleTag;

import java.util.List;

public interface TsRoleTagMapper extends BaseMapper<TsRoleTag> {
    List<TsRoleTag> selectEnabledRoleTags();
}

