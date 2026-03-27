package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;
import org.jeecg.modules.system.po.tschatmessageattachment.TsChatMessageAttachmentQueryPo;
public interface TsChatMessageAttachmentMapper extends BaseMapper<TsChatMessageAttachment> {
    Page<TsChatMessageAttachment> selectAttachmentPage(Page<TsChatMessageAttachment> page,
                                                       @Param("query") TsChatMessageAttachmentQueryPo query);
    TsChatMessageAttachment selectOwnedById(@Param("id") Long id, @Param("userId") String userId);
    Integer selectOwnedMessageCount(@Param("messageId") Long messageId, @Param("userId") String userId);
}