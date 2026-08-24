package org.jeecg.modules.system.review;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.system.entity.TsWorkReview;
import org.jeecg.modules.system.service.impl.ToolcallJsonRepairService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TsWorkAiReviewer {
    private static final String PROMPT_CODE = "ts_work_review";
    private static final String PROMPT_VERSION = "v1";

    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private ToolcallJsonRepairService toolcallJsonRepairService;

    public JSONObject review(TsWorkReview review) {
        Map<String, String> variables = new HashMap<>();
        variables.put("work_type", review.getWorkType());
        variables.put("snapshot_hash", review.getSnapshotHash());
        variables.put("snapshot_json", review.getSnapshotJson());
        PromptRenderedSectionsVo sections =
                promptRenderService.renderPromptSections(PROMPT_CODE, PROMPT_VERSION, variables);
        return toolcallJsonRepairService.chatToolCallWithSchemaRepair(sections, "work-review");
    }
}
