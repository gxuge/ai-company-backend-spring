package org.jeecg.modules.system.recommendetl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.config.TsRecommendEtlConfig;
import org.jeecg.modules.system.entity.TsRecommendEtlExecution;
import org.jeecg.modules.system.entity.TsRecommendEtlTask;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** 使用 ProcessBuilder 启动 Python ETL 脚本。 */
@Component
public class TsRecommendEtlProcessRunner {
    private static final Pattern PARAM_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]{0,63}");
    private static final Set<String> RESERVED_PARAMS = Set.of(
            "start", "end", "type", "output", "storage",
            "train-ratio", "eval-ratio");
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final TsRecommendEtlConfig config;
    private final ObjectMapper objectMapper;
    private final TsRecommendEtlResultParser resultParser;

    /** 注入运行配置和 JSON 组件。 */
    public TsRecommendEtlProcessRunner(
            TsRecommendEtlConfig config,
            ObjectMapper objectMapper,
            TsRecommendEtlResultParser resultParser) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.resultParser = resultParser;
    }

    /** 执行 Python 并返回结构化结果。 */
    public TsRecommendEtlProcessResult run(
            TsRecommendEtlTask task,
            TsRecommendEtlExecution execution) {
        long executionId = execution.getId();
        try {
            Path script = resolveAllowed(
                    config.getScriptRoot(), task.getScriptPath(), "脚本");
            if (!Files.isRegularFile(script)
                    || !script.getFileName().toString().toLowerCase(Locale.ROOT)
                    .endsWith(".py")) {
                return failure("SCRIPT_NOT_FOUND", "Python 脚本不存在或不是 .py 文件");
            }
            Path output = resolveAllowed(
                    config.getOutputRoot(),
                    Paths.get(task.getOutputDir(), String.valueOf(executionId)).toString(),
                    "输出");
            Files.createDirectories(output);
            Path logRoot = Paths.get(config.getLogRoot()).toAbsolutePath().normalize();
            Files.createDirectories(logRoot);
            Path logFile = logRoot.resolve(executionId + ".log").normalize();

            List<String> command = buildCommand(task, execution, script, output);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            AtomicReference<String> logContent = new AtomicReference<>("");
            AtomicReference<String> lastLine = new AtomicReference<>("");
            AtomicReference<IOException> readFailure = new AtomicReference<>();
            Thread reader = new Thread(
                    () -> readOutput(process, logFile, logContent, lastLine, readFailure),
                    "recommend-etl-output-" + executionId);
            reader.setDaemon(true);
            reader.start();

            int timeout = timeoutSeconds(task.getTimeoutSeconds());
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                reader.join(5000L);
                return failure("PROCESS_TIMEOUT",
                        "Python 执行超过 " + timeout + " 秒")
                        .setLogPath(logFile.toString())
                        .setLogContent(logContent.get());
            }
            reader.join(5000L);
            if (readFailure.get() != null) {
                return failure("PROCESS_LOG_READ_FAILED",
                        readFailure.get().getMessage())
                        .setExitCode(process.exitValue())
                        .setLogPath(logFile.toString())
                        .setLogContent(logContent.get());
            }
            return resultParser.parse(
                    lastLine.get(),
                    process.exitValue(),
                    logFile.toString(),
                    logContent.get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure("PROCESS_INTERRUPTED", "Python 执行线程被中断");
        } catch (Exception exception) {
            return failure("PROCESS_START_FAILED", exception.getMessage());
        }
    }

    /** 构造无 Shell 参与的参数数组。 */
    private List<String> buildCommand(
            TsRecommendEtlTask task,
            TsRecommendEtlExecution execution,
            Path script,
            Path output) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN);
        List<String> command = new ArrayList<>();
        command.add(config.getPythonExecutable());
        command.add(script.toString());
        command.add("--start=" + formatter.format(execution.getRangeStartTime()));
        command.add("--end=" + formatter.format(execution.getRangeEndTime()));
        command.add("--type=" + task.getRecommendType().toLowerCase(Locale.ROOT));
        command.add("--output=" + output);
        command.add("--storage=" + task.getStorageType().toLowerCase(Locale.ROOT));
        command.add("--train-ratio=" + decimal(task.getTrainRatio()));
        command.add("--eval-ratio=" + decimal(task.getEvalRatio()));
        for (Map.Entry<String, Object> entry : additionalParams(task).entrySet()) {
            command.add("--" + entry.getKey() + "=" + entry.getValue());
        }
        return command;
    }

    /** 解析并限制附加参数，只接受标量值和安全参数名。 */
    private Map<String, Object> additionalParams(TsRecommendEtlTask task)
            throws IOException {
        if (!StringUtils.hasText(task.getRunParamsJson())) {
            return Map.of();
        }
        Map<String, Object> values = objectMapper.readValue(
                task.getRunParamsJson(),
                new TypeReference<LinkedHashMap<String, Object>>() {
                });
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!PARAM_NAME.matcher(entry.getKey()).matches()
                    || RESERVED_PARAMS.contains(entry.getKey())) {
                throw new IllegalArgumentException(
                        "附加参数名不合法：" + entry.getKey());
            }
            Object value = entry.getValue();
            if (!(value instanceof String
                    || value instanceof Number
                    || value instanceof Boolean)) {
                throw new IllegalArgumentException(
                        "附加参数仅支持字符串、数字和布尔值");
            }
        }
        return values;
    }

    /** 持续读取标准输出，写完整日志并保留数据库日志尾部。 */
    private void readOutput(
            Process process,
            Path logFile,
            AtomicReference<String> logContent,
            AtomicReference<String> lastLine,
            AtomicReference<IOException> failure) {
        StringBuilder retained = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = Files.newBufferedWriter(
                     logFile,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                if (StringUtils.hasText(line)) {
                    lastLine.set(line);
                }
                appendTail(retained, line + System.lineSeparator());
            }
        } catch (IOException exception) {
            failure.set(exception);
        } finally {
            logContent.set(retained.toString());
        }
    }

    /** 保留日志尾部，避免单条记录无限增长。 */
    private void appendTail(StringBuilder target, String value) {
        target.append(value);
        int max = Math.max(config.getMaxLogChars(), 1000);
        if (target.length() > max) {
            target.delete(0, target.length() - max);
        }
    }

    /** 将相对路径约束到指定根目录内。 */
    private Path resolveAllowed(String rootValue, String value, String label) {
        Path root = Paths.get(rootValue).toAbsolutePath().normalize();
        Path candidate = Paths.get(value);
        Path resolved = candidate.isAbsolute()
                ? candidate.toAbsolutePath().normalize()
                : root.resolve(candidate).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(label + "路径超出允许根目录");
        }
        return resolved;
    }

    /** 归一化任务超时时间。 */
    private int timeoutSeconds(Integer value) {
        int timeout = value == null
                ? config.getDefaultTimeoutSeconds()
                : value;
        return Math.min(Math.max(timeout, 10), config.getMaxTimeoutSeconds());
    }

    /** 输出不带科学计数法的小数。 */
    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /** 创建启动前失败结果。 */
    private TsRecommendEtlProcessResult failure(String code, String message) {
        return new TsRecommendEtlProcessResult()
                .setSuccess(false)
                .setErrorCode(code)
                .setErrorMessage(message);
    }
}
