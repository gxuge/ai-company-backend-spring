package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentQueryDto;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentSaveDto;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;
import org.jeecg.modules.system.vo.tschatmessageattachment.TsChatMessageAttachmentVo;
public interface ITsChatMessageAttachmentService extends IService<TsChatMessageAttachment> {
    Result<Page<TsChatMessageAttachmentVo>> pageAttachments(LoginUser user, TsChatMessageAttachmentQueryDto request);
    Result<TsChatMessageAttachmentVo> getAttachment(LoginUser user, Long id);
    Result<TsChatMessageAttachmentVo> addAttachment(LoginUser user, TsChatMessageAttachmentSaveDto request);
    Result<TsChatMessageAttachmentVo> editAttachment(LoginUser user, Long id, TsChatMessageAttachmentSaveDto request);
    Result<?> deleteAttachment(LoginUser user, Long id);
}