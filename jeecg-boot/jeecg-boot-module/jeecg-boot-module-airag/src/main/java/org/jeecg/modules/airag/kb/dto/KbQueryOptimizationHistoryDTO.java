package org.jeecg.modules.airag.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 查询优化的单条历史记录。
 */
@Data
@Schema(description = "查询优化的单条历史记录")
public class KbQueryOptimizationHistoryDTO {
    /**
     * 角色：user/assistant/system。
     */
    @NotBlank(message = "chat_history.role不能为空")
    @Pattern(regexp = "^(user|assistant|system)$", message = "chat_history.role只能是user、assistant、system")
    @Schema(description = "角色：user/assistant/system", requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;

    /**
     * 内容。
     */
    @NotBlank(message = "chat_history.content不能为空")
    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
