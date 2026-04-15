package org.jeecg.modules.system.dto.tsuservoiceprofile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TsUserVoiceProfileRenameDto {

    @NotBlank(message = "name cannot be blank")
    @Size(max = 50, message = "name length cannot exceed 50")
    private String name;

    public void normalize() {
        if (this.name == null) {
            return;
        }
        this.name = this.name.trim();
    }
}

