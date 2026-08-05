package org.jeecg.modules.system.dto.tsuserimageasset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TsUserImageAssetImportDto {
    @NotBlank(message = "原始图片URL不能为空")
    private String sourceImageUrl;
    private String fileName;
    private String sourceType;
    @Size(max = 64, message = "来源标识长度不能超过64个字符")
    private String sourceKey;
}
