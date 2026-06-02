package org.jeecg.modules.system.vo.tsailog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.system.entity.TsAiLog;
import org.jeecg.modules.system.entity.TsAiLogStep;

import java.util.List;

@Data
@Schema(description = "ts AI调用监控详情")
public class TsAiLogDetailVo {

    @Schema(description = "主日志")
    private TsAiLog log;

    @Schema(description = "步骤列表")
    private List<TsAiLogStep> steps;
}
