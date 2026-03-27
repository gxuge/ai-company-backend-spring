package org.jeecg.modules.system.dto.tsvoicetag;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
@Data
public class TsVoiceTagSaveDto {
    @NotBlank(message = "tagName不能为空")
    private String tagName;
}
