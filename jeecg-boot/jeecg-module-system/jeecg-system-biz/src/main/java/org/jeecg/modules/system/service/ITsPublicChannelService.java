package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspublicchannel.TsPublicChannelQueryDto;
import org.jeecg.modules.system.dto.tspublicchannel.TsPublicChannelSaveDto;
import org.jeecg.modules.system.entity.TsPublicChannel;
import org.jeecg.modules.system.vo.tspublicchannel.TsPublicChannelOptionVo;
import org.jeecg.modules.system.vo.tspublicchannel.TsPublicChannelVo;

import java.util.List;

/**
 * 公开渠道 Service。
 */
public interface ITsPublicChannelService extends IService<TsPublicChannel> {
    Result<Page<TsPublicChannelVo>> pageChannels(LoginUser user, TsPublicChannelQueryDto request);
    Result<TsPublicChannelVo> getChannel(LoginUser user, Long id);
    Result<TsPublicChannelVo> addChannel(LoginUser user, TsPublicChannelSaveDto request);
    Result<TsPublicChannelVo> editChannel(LoginUser user, Long id, TsPublicChannelSaveDto request);
    Result<?> deleteChannel(LoginUser user, Long id);
    Result<List<TsPublicChannelOptionVo>> listChannelOptions(LoginUser user, String targetType);
}
