package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsChatMessageOwnershipAspect;
import org.jeecg.modules.aop.TsChatMessageOwnershipAspect.CheckTsChatMessageOwnership;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageQueryDto;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageSaveDto;
import org.jeecg.modules.system.entity.TsChatMessage;
import org.jeecg.modules.system.mapper.TsChatMessageMapper;
import org.jeecg.modules.system.po.tschatmessage.TsChatMessageQueryPo;
import org.jeecg.modules.system.po.tschatmessage.TsChatMessageSavePo;
import org.jeecg.modules.system.service.ITsChatMessageService;
import org.jeecg.modules.system.vo.tschatmessage.TsChatMessageVo;
import org.jeecg.modules.system.vo.tschatmessage.TsChatMessageVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class TsChatMessageServiceImpl extends ServiceImpl<TsChatMessageMapper, TsChatMessage>
        implements ITsChatMessageService {
    @Override
    public Result<Page<TsChatMessageVo>> pageMessages(LoginUser user, TsChatMessageQueryDto request) {
        TsChatMessageQueryPo queryPo = TsChatMessageQueryPo.fromRequest(user.getId(), request);
        Page<TsChatMessage> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsChatMessage> pageData = baseMapper.selectMessagePage(page, queryPo);
        return Result.OK(TsChatMessageVoConverter.fromPage(pageData));
    }
    @Override
    @CheckTsChatMessageOwnership(message = "消息不存在或无权限访问")
    public Result<TsChatMessageVo> getMessage(LoginUser user, Long id) {
        TsChatMessage record = TsChatMessageOwnershipAspect.MESSAGE_CONTEXT.get();
        return Result.OK(TsChatMessageVoConverter.fromEntity(record));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsChatMessageVo> addMessage(LoginUser user, TsChatMessageSaveDto request) {
        request.applyCreateDefaults();

        Integer sessionCount = baseMapper.selectOwnedSessionCount(request.getSessionId(), user.getId());
        if (sessionCount == null || sessionCount < 1) {
            throw new JeecgBootException("会话不存在或无权限访问");
        }

        if (request.getReplyToMessageId() != null) {
            TsChatMessage reply = baseMapper.selectOwnedById(request.getReplyToMessageId(), user.getId());
            if (reply == null) {
                throw new JeecgBootException("被回复消息不存在或无权限访问");
            }
            if (!request.getSessionId().equals(reply.getSessionId())) {
                throw new JeecgBootException("被回复消息不属于当前会话");
            }
        }

        TsChatMessageSavePo savePo = TsChatMessageSavePo.fromRequest(request);
        TsChatMessage entity = new TsChatMessage();
        savePo.applyCreateTo(entity);
        entity.setSeqNo(baseMapper.selectNextSeqNoForUpdate(request.getSessionId()));
        entity.setCreatedAt(new Date());
        this.save(entity);

        return Result.OK("创建成功", TsChatMessageVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatMessageOwnership(message = "消息不存在或无权限访问")
    public Result<TsChatMessageVo> editMessage(LoginUser user, Long id, TsChatMessageSaveDto request) {
        TsChatMessage record = TsChatMessageOwnershipAspect.MESSAGE_CONTEXT.get();

        if (request.getSessionId() != null && !request.getSessionId().equals(record.getSessionId())) {
            throw new JeecgBootException("不允许修改消息所属会话");
        }

        if (request.getReplyToMessageId() != null) {
            TsChatMessage reply = baseMapper.selectOwnedById(request.getReplyToMessageId(), user.getId());
            if (reply == null) {
                throw new JeecgBootException("被回复消息不存在或无权限访问");
            }
            if (!record.getSessionId().equals(reply.getSessionId())) {
                throw new JeecgBootException("被回复消息不属于当前会话");
            }
        }

        TsChatMessageSavePo savePo = TsChatMessageSavePo.fromRequest(request);
        savePo.applyUpdateTo(record);
        this.updateById(record);

        return Result.OK("更新成功", TsChatMessageVoConverter.fromEntity(record));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatMessageOwnership(message = "消息不存在或无权限访问")
    public Result<?> deleteMessage(LoginUser user, Long id) {
        TsChatMessage record = TsChatMessageOwnershipAspect.MESSAGE_CONTEXT.get();
        this.removeById(record.getId());
        return Result.OK("删除成功");
    }
}
