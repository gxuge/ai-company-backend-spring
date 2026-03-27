package org.jeecg.modules.system.dto.tsroleimageprofile;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
@Data
public class TsRoleImageProfileSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑模板时id不能为空", groups = Update.class)
    private Long id;
    private String profileName;
    private String promptText;
    private String styleName;
    private Long selectedImageAssetId;
    private String selectedImageUrl;
    private String sourceType;
    private Integer isPublic;
    private Integer status;
    private String extJson;
    public void applyCreateDefaults() {
        if (this.sourceType == null || this.sourceType.trim().isEmpty()) {
            this.sourceType = "ai_generate";
        }
        if (this.isPublic == null) {
            this.isPublic = 1;
        }
        if (this.status == null) {
            this.status = 1;
        }
    }
}