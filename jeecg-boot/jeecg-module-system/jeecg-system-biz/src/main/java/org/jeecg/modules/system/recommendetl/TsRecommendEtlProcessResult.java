package org.jeecg.modules.system.recommendetl;

import lombok.Data;
import lombok.experimental.Accessors;

/** Python ETL 进程执行结果。 */
@Data
@Accessors(chain = true)
public class TsRecommendEtlProcessResult {
    /** 是否成功。 */
    private boolean success;
    /** 进程退出码。 */
    private Integer exitCode;
    /** train 数量。 */
    private Long trainCount;
    /** eval 数量。 */
    private Long evalCount;
    /** 正样本数量。 */
    private Long positiveCount;
    /** 负样本数量。 */
    private Long negativeCount;
    /** train 文件路径。 */
    private String trainPath;
    /** eval 文件路径。 */
    private String evalPath;
    /** 原始结果 JSON。 */
    private String resultJson;
    /** 完整日志路径。 */
    private String logPath;
    /** 截断后的日志内容。 */
    private String logContent;
    /** 错误码。 */
    private String errorCode;
    /** 错误信息。 */
    private String errorMessage;
}
