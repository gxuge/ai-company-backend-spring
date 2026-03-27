package org.jeecg.modules.system.dto.tsroleimagegeneraterecord;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
@Data
public class TsRoleImageGenerateRecordSaveDto {
    public interface Create {}
    public interface Update {}
    @NotNull(message = "编辑记录时id不能为空", groups = Update.class)
    private Long id;
    @NotNull(message = "角色ID不能为空", groups = Create.class)
    private Long roleId;
    private String sourceProfileUrl;
    private String promptText;
    private String styleName;
    private String referenceAssetsJson;
    private String generateStatus;
    private String applyStatus;
    private Long resultAssetId;
    private String resultImageUrl;
    private String failReason;
    private String requestId;
    private String extJson;
    public void applyCreateDefaults() {
        if (this.generateStatus == null || this.generateStatus.trim().isEmpty()) {
            this.generateStatus = "pending";
        }
        if (this.applyStatus == null || this.applyStatus.trim().isEmpty()) {
            this.applyStatus = "pending";
        }
    }
}