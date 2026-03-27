package org.jeecg.modules.system.vo.tschatmessageattachment;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;

import java.util.ArrayList;
import java.util.List;
public final class TsChatMessageAttachmentVoConverter {

    private TsChatMessageAttachmentVoConverter() {
    }
    public static Page<TsChatMessageAttachmentVo> fromPage(Page<TsChatMessageAttachment> source) {
        Page<TsChatMessageAttachmentVo> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<TsChatMessageAttachmentVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsChatMessageAttachment item : source.getRecords()) {
                records.add(fromEntity(item));
            }
        }
        target.setRecords(records);
        return target;
    }
    public static TsChatMessageAttachmentVo fromEntity(TsChatMessageAttachment source) {
        if (source == null) {
            return null;
        }
        TsChatMessageAttachmentVo target = new TsChatMessageAttachmentVo();
        target.setId(source.getId());
        target.setMessageId(source.getMessageId());
        target.setFileType(source.getFileType());
        target.setFileUrl(source.getFileUrl());
        target.setFileName(source.getFileName());
        target.setFileSize(source.getFileSize());
        target.setDurationSec(source.getDurationSec());
        target.setMimeType(source.getMimeType());
        target.setCreatedAt(source.getCreatedAt());
        return target;
    }
}