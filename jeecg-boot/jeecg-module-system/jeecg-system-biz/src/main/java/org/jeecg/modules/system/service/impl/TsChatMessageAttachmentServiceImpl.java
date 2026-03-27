package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsChatMessageAttachmentOwnershipAspect;
import org.jeecg.modules.aop.TsChatMessageAttachmentOwnershipAspect.CheckTsChatMessageAttachmentOwnership;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentQueryDto;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentSaveDto;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;
import org.jeecg.modules.system.mapper.TsChatMessageAttachmentMapper;
import org.jeecg.modules.system.po.tschatmessageattachment.TsChatMessageAttachmentQueryPo;
import org.jeecg.modules.system.po.tschatmessageattachment.TsChatMessageAttachmentSavePo;
import org.jeecg.modules.system.service.ITsChatMessageAttachmentService;
import org.jeecg.modules.system.vo.tschatmessageattachment.TsChatMessageAttachmentVo;
import org.jeecg.modules.system.vo.tschatmessageattachment.TsChatMessageAttachmentVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Service
public class TsChatMessageAttachmentServiceImpl
        extends ServiceImpl<TsChatMessageAttachmentMapper, TsChatMessageAttachment>
        implements ITsChatMessageAttachmentService {
    @Override
    public Result<Page<TsChatMessageAttachmentVo>> pageAttachments(LoginUser user, TsChatMessageAttachmentQueryDto request) {
        TsChatMessageAttachmentQueryPo queryPo = TsChatMessageAttachmentQueryPo.fromRequest(user.getId(), request);
        Page<TsChatMessageAttachment> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsChatMessageAttachment> pageData = baseMapper.selectAttachmentPage(page, queryPo);
        return Result.OK(TsChatMessageAttachmentVoConverter.fromPage(pageData));
    }
    @Override
    @CheckTsChatMessageAttachmentOwnership(message = "消息附件不存在或无权限访问")
    public Result<TsChatMessageAttachmentVo> getAttachment(LoginUser user, Long id) {
        TsChatMessageAttachment record = TsChatMessageAttachmentOwnershipAspect.ATTACHMENT_CONTEXT.get();
        return Result.OK(TsChatMessageAttachmentVoConverter.fromEntity(record));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsChatMessageAttachmentVo> addAttachment(LoginUser user, TsChatMessageAttachmentSaveDto request) {
        Integer owned = baseMapper.selectOwnedMessageCount(request.getMessageId(), user.getId());
        if (owned == null || owned < 1) {
            throw new JeecgBootException("消息不存在或无权限访问");
        }

        TsChatMessageAttachmentSavePo savePo = TsChatMessageAttachmentSavePo.fromRequest(request);
        TsChatMessageAttachment entity = new TsChatMessageAttachment();
        savePo.applyCreateTo(entity);
        entity.setCreatedAt(new Date());
        this.save(entity);

        return Result.OK("创建成功", TsChatMessageAttachmentVoConverter.fromEntity(entity));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatMessageAttachmentOwnership(message = "消息附件不存在或无权限访问")
    public Result<TsChatMessageAttachmentVo> editAttachment(LoginUser user, Long id, TsChatMessageAttachmentSaveDto request) {
        TsChatMessageAttachment record = TsChatMessageAttachmentOwnershipAspect.ATTACHMENT_CONTEXT.get();

        if (request.getMessageId() != null && !request.getMessageId().equals(record.getMessageId())) {
            throw new JeecgBootException("不允许修改附件所属消息");
        }

        TsChatMessageAttachmentSavePo savePo = TsChatMessageAttachmentSavePo.fromRequest(request);
        savePo.applyUpdateTo(record);
        this.updateById(record);

        return Result.OK("更新成功", TsChatMessageAttachmentVoConverter.fromEntity(record));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsChatMessageAttachmentOwnership(message = "消息附件不存在或无权限访问")
    public Result<?> deleteAttachment(LoginUser user, Long id) {
        TsChatMessageAttachment record = TsChatMessageAttachmentOwnershipAspect.ATTACHMENT_CONTEXT.get();
        this.removeById(record.getId());
        return Result.OK("删除成功");
    }
}