package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsRoleTag;
import org.jeecg.modules.system.vo.tsroletag.TsRoleTagVo;

import java.util.List;

public interface ITsRoleTagService extends IService<TsRoleTag> {
    Result<List<TsRoleTagVo>> listRoleTags(LoginUser user);
}

