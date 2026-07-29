package org.jeecg.modules.system.dto.tsuserimageasset;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TsUserImageAssetImportDto {
    @NotBlank(message = "原始图片URL不能为空")
    private String sourceImageUrl;
    private String fileName;
    private String sourceType;
}
