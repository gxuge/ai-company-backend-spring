package org.jeecg.modules.system.po.tschatmessageattachment;

import lombok.Data;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentSaveDto;
import org.jeecg.modules.system.entity.TsChatMessageAttachment;
@Data
public class TsChatMessageAttachmentSavePo {
    private Long messageId;
    private String fileType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Integer durationSec;
    private String mimeType;
    public static TsChatMessageAttachmentSavePo fromRequest(TsChatMessageAttachmentSaveDto request) {
        TsChatMessageAttachmentSavePo po = new TsChatMessageAttachmentSavePo();
        if (request == null) {
            return po;
        }
        po.setMessageId(request.getMessageId());
        po.setFileType(trimToNull(request.getFileType()));
        po.setFileUrl(trimToNull(request.getFileUrl()));
        po.setFileName(trimToNull(request.getFileName()));
        po.setFileSize(request.getFileSize());
        po.setDurationSec(request.getDurationSec());
        po.setMimeType(trimToNull(request.getMimeType()));
        return po;
    }
    public void applyCreateTo(TsChatMessageAttachment entity) {
        if (entity == null) {
            return;
        }
        entity.setMessageId(this.messageId);
        entity.setFileType(this.fileType);
        entity.setFileUrl(this.fileUrl);
        entity.setFileName(this.fileName);
        entity.setFileSize(this.fileSize);
        entity.setDurationSec(this.durationSec);
        entity.setMimeType(this.mimeType);
    }
    public void applyUpdateTo(TsChatMessageAttachment entity) {
        if (entity == null) {
            return;
        }
        if (this.fileType != null) {
            entity.setFileType(this.fileType);
        }
        if (this.fileUrl != null) {
            entity.setFileUrl(this.fileUrl);
        }
        entity.setFileName(this.fileName);
        entity.setFileSize(this.fileSize);
        entity.setDurationSec(this.durationSec);
        entity.setMimeType(this.mimeType);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}