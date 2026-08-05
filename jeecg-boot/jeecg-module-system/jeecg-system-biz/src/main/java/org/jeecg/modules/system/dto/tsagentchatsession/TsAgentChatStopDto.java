package org.jeecg.modules.system.dto.tsagentchatsession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Stops one active Agent reply run.
 */
@Data
public class TsAgentChatStopDto {

    @NotNull(message = "sessionId不能为空")
    private Long sessionId;

    @NotBlank(message = "runId不能为空")
    private String runId;
}
