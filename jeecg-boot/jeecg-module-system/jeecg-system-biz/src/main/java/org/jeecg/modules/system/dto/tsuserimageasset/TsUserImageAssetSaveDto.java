package org.jeecg.modules.system.dto.tsuserimageasset;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Data
public class TsUserImageAssetSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑素材时id不能为空", groups = Update.class)
    private Long id;
    @NotBlank(message = "原图URL不能为空", groups = {Create.class, Update.class})
    private String fileUrl;
    private String thumbnailUrl;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    @NotBlank(message = "素材来源不能为空", groups = {Create.class, Update.class})
    private String sourceType;
    private Integer status;
    public void applyCreateDefaults() {
        if (this.status == null) {
            this.status = 1;
        }
        if (this.sourceType == null || this.sourceType.trim().isEmpty()) {
            this.sourceType = "upload";
        }
    }
}
