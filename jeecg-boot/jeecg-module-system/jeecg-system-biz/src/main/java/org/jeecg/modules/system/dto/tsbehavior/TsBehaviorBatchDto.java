package org.jeecg.modules.system.dto.tsbehavior;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 推荐行为批量上报参数。 */
@Data
public class TsBehaviorBatchDto {
    /** 行为事件列表，单批最多100条。 */
    @Valid
    @NotEmpty
    @Size(max = 100)
    private List<TsBehaviorEventDto> events;
}
