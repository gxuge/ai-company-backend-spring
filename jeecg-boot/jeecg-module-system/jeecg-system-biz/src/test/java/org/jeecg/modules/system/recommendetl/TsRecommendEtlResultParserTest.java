package org.jeecg.modules.system.recommendetl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 推荐 ETL Python 结果解析器测试。 */
class TsRecommendEtlResultParserTest {
    private final TsRecommendEtlResultParser parser =
            new TsRecommendEtlResultParser(new ObjectMapper());

    /** 合法成功 JSON 应完整转换统计与路径。 */
    @Test
    void shouldParseSuccessResult() {
        String json = """
                {"success":true,"train_count":100,"eval_count":10,
                "positive_count":30,"negative_count":80,
                "train_path":"train.csv","eval_path":"eval.csv"}
                """.replace(System.lineSeparator(), "");

        TsRecommendEtlProcessResult result =
                parser.parse(json, 0, "run.log", "ok");

        assertTrue(result.isSuccess());
        assertEquals(100L, result.getTrainCount());
        assertEquals(10L, result.getEvalCount());
        assertEquals("train.csv", result.getTrainPath());
    }

    /** 非零退出码必须覆盖 success=true 并记录机器错误码。 */
    @Test
    void shouldFailWhenProcessExitCodeIsNonZero() {
        TsRecommendEtlProcessResult result = parser.parse(
                "{\"success\":true}",
                2,
                "run.log",
                "failed");

        assertFalse(result.isSuccess());
        assertEquals("PROCESS_EXIT_NON_ZERO", result.getErrorCode());
    }

    /** 缺失必须统计字段时应返回结构化失败。 */
    @Test
    void shouldFailWhenRequiredFieldIsMissing() {
        TsRecommendEtlProcessResult result = parser.parse(
                "{\"success\":true,\"train_count\":1}",
                0,
                "run.log",
                "invalid");

        assertFalse(result.isSuccess());
        assertEquals("RESULT_JSON_INVALID", result.getErrorCode());
    }
}
