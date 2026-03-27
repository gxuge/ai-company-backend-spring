package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageQueryDto;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageSaveDto;
import org.jeecg.modules.system.entity.TsChatMessage;
import org.jeecg.modules.system.vo.tschatmessage.TsChatMessageVo;
public interface ITsChatMessageService extends IService<TsChatMessage> {
    Result<Page<TsChatMessageVo>> pageMessages(LoginUser user, TsChatMessageQueryDto request);
    Result<TsChatMessageVo> getMessage(LoginUser user, Long id);
    Result<TsChatMessageVo> addMessage(LoginUser user, TsChatMessageSaveDto request);
    Result<TsChatMessageVo> editMessage(LoginUser user, Long id, TsChatMessageSaveDto request);
    Result<?> deleteMessage(LoginUser user, Long id);
}
