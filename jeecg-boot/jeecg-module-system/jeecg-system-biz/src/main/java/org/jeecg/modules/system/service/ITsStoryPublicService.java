package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicActionDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicQueryDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicSaveDto;
import org.jeecg.modules.system.entity.TsStoryPublic;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicTargetOptionVo;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicVo;

/**
 * 故事公开记录 Service。
 */
public interface ITsStoryPublicService extends IService<TsStoryPublic> {
    Result<Page<TsStoryPublicVo>> pagePublics(LoginUser user, TsStoryPublicQueryDto request);
    Result<TsStoryPublicVo> getPublic(LoginUser user, Long id);
    Result<TsStoryPublicVo> addPublic(LoginUser user, TsStoryPublicSaveDto request);
    Result<TsStoryPublicVo> editPublic(LoginUser user, Long id, TsStoryPublicSaveDto request);
    Result<?> deletePublic(LoginUser user, Long id);
    Result<TsStoryPublicVo> submitPublic(LoginUser user, TsStoryPublicActionDto request);
    Result<TsStoryPublicVo> approvePublic(LoginUser user, TsStoryPublicActionDto request);
    Result<TsStoryPublicVo> rejectPublic(LoginUser user, TsStoryPublicActionDto request);
    Result<TsStoryPublicVo> onlinePublic(LoginUser user, TsStoryPublicActionDto request);
    Result<TsStoryPublicVo> offlinePublic(LoginUser user, TsStoryPublicActionDto request);
    Result<Page<TsStoryPublicTargetOptionVo>> pageStoryOptions(LoginUser user, String ownerUserId, String keyword, Integer pageNo, Integer pageSize);
}
