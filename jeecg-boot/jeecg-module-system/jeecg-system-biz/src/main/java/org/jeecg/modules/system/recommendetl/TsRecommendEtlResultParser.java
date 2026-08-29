package org.jeecg.modules.system.recommendetl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Python ETL 最终 JSON 结果解析器。 */
@Component
public class TsRecommendEtlResultParser {
    private final ObjectMapper objectMapper;

    /** 注入 JSON 解析器。 */
    public TsRecommendEtlResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 解析最后一条非空标准输出，字段不合法时返回失败结果。 */
    public TsRecommendEtlProcessResult parse(
            String lastLine,
            int exitCode,
            String logPath,
            String logContent) {
        TsRecommendEtlProcessResult result = new TsRecommendEtlProcessResult()
                .setExitCode(exitCode)
                .setLogPath(logPath)
                .setLogContent(logContent);
        if (!StringUtils.hasText(lastLine)) {
            return fail(result, "RESULT_JSON_MISSING", "Python 未返回结果 JSON");
        }
        try {
            JsonNode root = objectMapper.readTree(lastLine);
            result.setResultJson(lastLine);
            if (exitCode != 0) {
                return fail(result, "PROCESS_EXIT_NON_ZERO",
                        "Python 进程退出码为 " + exitCode);
            }
            if (!root.path("success").asBoolean(false)) {
                String message = root.path("error").asText("Python 返回执行失败");
                return fail(result, "PYTHON_RESULT_FAILED", message);
            }
            result.setSuccess(true)
                    .setTrainCount(requiredLong(root, "train_count"))
                    .setEvalCount(requiredLong(root, "eval_count"))
                    .setPositiveCount(requiredLong(root, "positive_count"))
                    .setNegativeCount(requiredLong(root, "negative_count"))
                    .setTrainPath(requiredText(root, "train_path"))
                    .setEvalPath(requiredText(root, "eval_path"));
            return result;
        } catch (Exception exception) {
            return fail(result, "RESULT_JSON_INVALID",
                    "Python 结果 JSON 不合法：" + exception.getMessage());
        }
    }

    /** 读取必须存在的非负整数。 */
    private long requiredLong(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.canConvertToLong() || value.asLong() < 0) {
            throw new IllegalArgumentException(field + " 必须是非负整数");
        }
        return value.asLong();
    }

    /** 读取必须存在的字符串。 */
    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !StringUtils.hasText(value.asText())) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.asText();
    }

    /** 填充失败结果。 */
    private TsRecommendEtlProcessResult fail(
            TsRecommendEtlProcessResult result,
            String code,
            String message) {
        return result.setSuccess(false).setErrorCode(code).setErrorMessage(message);
    }
}
