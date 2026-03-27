package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect;
import org.jeecg.modules.aop.TsChatSessionOwnershipAspect.CheckTsChatSessionOwnership;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionQueryDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionSaveDto;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.mapper.TsChatSessionMapper;
import org.jeecg.modules.system.po.tschatsession.TsChatSessionQueryPo;
import org.jeecg.modules.system.po.tschatsession.TsChatSessionSavePo;
import org.jeecg.modules.system.service.ITsChatSessionService;
import org.jeecg.modules.system.vo.tschatsession.TsChatSessionVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatSessionVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class TsChatSessionServiceImpl extends ServiceImpl<TsChatSessionMapper, TsChatSession>
        implements ITsChatSessionService {
    @Override
    public Result<Page<TsChatSessionVo>> pageSessions(LoginUser user, TsChatSessionQueryDto request) {
        TsChatSessionQueryPo queryPo = TsChatSessionQueryPo.fromRequest(user.getId(), request);
        Page<TsChatSession> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsChatSession> pageData = baseMapper.selectSessionPage(page, queryPo);
        return Result.OK(TsChatSessionVoConverter.fromPage(pageData));
    }
    @Override
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatSessionVo> getSession(LoginUser user, Long id) {
        TsChatSession record = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        return Result.OK(TsChatSessionVoConverter.fromEntity(record));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsChatSessionVo> addSession(LoginUser user, TsChatSessionSaveDto request) {
        request.applyCreateDefaults();

        TsChatSessionSavePo savePo = TsChatSessionSavePo.fromRequest(request);
        TsChatSession entity = new TsChatSession();
        savePo.applyCreateTo(entity);
        entity.setUserId(user.getId());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        this.save(entity);

        return Result.OK("创建成功", TsChatSessionVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<TsChatSessionVo> editSession(LoginUser user, Long id, TsChatSessionSaveDto request) {
        TsChatSession record = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();

        TsChatSessionSavePo savePo = TsChatSessionSavePo.fromRequest(request);
        savePo.applyUpdateTo(record);
        record.setUpdatedAt(new Date());
        this.updateById(record);

        return Result.OK("更新成功", TsChatSessionVoConverter.fromEntity(record));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatSessionOwnership(message = "会话不存在或无权限访问")
    public Result<?> deleteSession(LoginUser user, Long id) {
        TsChatSession record = TsChatSessionOwnershipAspect.SESSION_CONTEXT.get();
        this.removeById(record.getId());
        return Result.OK("删除成功");
    }
}
