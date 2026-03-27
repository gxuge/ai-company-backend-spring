package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionQueryDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionSaveDto;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.vo.tschatsession.TsChatSessionVo;
public interface ITsChatSessionService extends IService<TsChatSession> {
    Result<Page<TsChatSessionVo>> pageSessions(LoginUser user, TsChatSessionQueryDto request);
    Result<TsChatSessionVo> getSession(LoginUser user, Long id);
    Result<TsChatSessionVo> addSession(LoginUser user, TsChatSessionSaveDto request);
    Result<TsChatSessionVo> editSession(LoginUser user, Long id, TsChatSessionSaveDto request);
    Result<?> deleteSession(LoginUser user, Long id);
}
