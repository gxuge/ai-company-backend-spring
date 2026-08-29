package org.jeecg.modules.system.service;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsbehavior.TsBehaviorEventDto;
import org.jeecg.modules.system.vo.tsbehavior.TsBehaviorCollectVo;

import java.util.List;

/** 业务行为采集服务。 */
public interface ITsBehaviorEventService {

    /** 校验并异步提交一批登录用户行为。 */
    TsBehaviorCollectVo collect(LoginUser loginUser, List<TsBehaviorEventDto> events);
}
