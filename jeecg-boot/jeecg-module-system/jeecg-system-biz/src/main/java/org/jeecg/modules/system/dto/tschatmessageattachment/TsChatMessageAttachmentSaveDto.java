package org.jeecg.modules.system.dto.tschatmessageattachment;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Data
public class TsChatMessageAttachmentSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑附件时id不能为空", groups = Update.class)
    private Long id;
    @NotNull(message = "messageId不能为空", groups = Create.class)
    private Long messageId;
    @NotBlank(message = "fileType不能为空", groups = Create.class)
    private String fileType;
    @NotBlank(message = "fileUrl不能为空", groups = Create.class)
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Integer durationSec;
    private String mimeType;
}