package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsRoleTag;
import org.jeecg.modules.system.mapper.TsRoleTagMapper;
import org.jeecg.modules.system.service.ITsRoleTagService;
import org.jeecg.modules.system.vo.tsroletag.TsRoleTagVo;
import org.jeecg.modules.system.vo.tsroletag.TsRoleTagVoConverter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TsRoleTagServiceImpl extends ServiceImpl<TsRoleTagMapper, TsRoleTag>
        implements ITsRoleTagService {

    @Override
    public Result<List<TsRoleTagVo>> listRoleTags(LoginUser user) {
        List<TsRoleTag> roleTags = baseMapper.selectEnabledRoleTags();
        return Result.OK(TsRoleTagVoConverter.fromEntityList(roleTags));
    }
}

