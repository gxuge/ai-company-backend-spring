package org.jeecg.modules.system.tagging;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.system.dto.tscontenttag.TsContentTagCandidateDto;
import org.jeecg.modules.system.service.ITsContentTagService;
import org.jeecg.modules.system.service.impl.ToolcallJsonRepairService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 使用可配置 Prompt 对审核快照执行固定词典分类。 */
@Component
public class TsContentTagAiClassifier {
    public static final String PROMPT_CODE = "ts_content_tagging";
    public static final String PROMPT_VERSION = "v1";

    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private ToolcallJsonRepairService toolcallJsonRepairService;
    @Resource
    private ITsContentTagService tsContentTagService;

    /** 调用模型并解析候选标签，最终合法性由内容标签服务再次校验。 */
    public List<TsContentTagCandidateDto> classify(String contentType, String snapshotJson) {
        Map<String, String> variables = new HashMap<>();
        variables.put("content_type", contentType);
        variables.put("tag_dictionary_json", tsContentTagService.buildDictionaryJson(contentType));
        variables.put("snapshot_json", snapshotJson);
        PromptRenderedSectionsVo sections =
                promptRenderService.renderPromptSections(PROMPT_CODE, PROMPT_VERSION, variables);
        JSONObject result = toolcallJsonRepairService.chatToolCallWithSchemaRepair(sections, "content-tagging");
        return tsContentTagService.parseCandidates(result == null ? null : result.get("tags"));
    }
}
