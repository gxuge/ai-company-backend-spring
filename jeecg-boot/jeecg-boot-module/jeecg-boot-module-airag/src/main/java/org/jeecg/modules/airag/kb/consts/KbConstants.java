package org.jeecg.modules.airag.kb.consts;

import java.math.BigDecimal;

/**
 * 知识库功能常量。
 */
public final class KbConstants {
    /**
     * 启用状态。
     */
    public static final Integer STATUS_ENABLE = 1;

    /**
     * 禁用状态。
     */
    public static final Integer STATUS_DISABLE = 0;

    /**
     * 默认知识库业务类型。
     */
    public static final String DEFAULT_BIZ_TYPE = "default";

    /**
     * 文档来源类型：手工录入。
     */
    public static final String SOURCE_TYPE_MANUAL = "manual";

    /**
     * 文档来源类型：文件上传。
     */
    public static final String SOURCE_TYPE_UPLOAD = "upload";

    /**
     * 文档来源类型：文件。
     */
    public static final String SOURCE_TYPE_FILE = "file";

    /**
     * 文档来源类型：外部URL。
     */
    public static final String SOURCE_TYPE_URL = "url";

    /**
     * 文档来源类型：外部知识库。
     */
    public static final String SOURCE_TYPE_EXTERNAL = "external";

    /**
     * 文档来源类型：导入。
     */
    public static final String SOURCE_TYPE_IMPORT = "import";

    /**
     * 文档来源类型：QA。
     */
    public static final String SOURCE_TYPE_QA = "qa";

    /**
     * 解析状态：等待处理。
     */
    public static final String PROCESS_STATUS_PENDING = "pending";

    /**
     * 解析状态：处理中。
     */
    public static final String PROCESS_STATUS_PROCESSING = "processing";

    /**
     * 解析状态：成功。
     */
    public static final String PROCESS_STATUS_SUCCESS = "success";

    /**
     * 解析状态：失败。
     */
    public static final String PROCESS_STATUS_FAILED = "failed";

    /**
     * 默认切分类型。
     */
    public static final String CHUNK_TYPE_TEXT = "text";

    /**
     * QA切分类型。
     */
    public static final String CHUNK_TYPE_QA = "qa";

    /**
     * 默认索引类型。
     */
    public static final String INDEX_TYPE_DEFAULT = "default";

    /**
     * 手工索引类型。
     */
    public static final String INDEX_TYPE_MANUAL = "manual";

    /**
     * 问题索引类型。
     */
    public static final String INDEX_TYPE_QUESTION = "question";

    /**
     * 关键词索引类型。
     */
    public static final String INDEX_TYPE_KEYWORD = "keyword";

    /**
     * 自动问题索引类型。
     */
    public static final String INDEX_TYPE_AUTO_QUESTION = "auto_question";

    /**
     * 默认自动问题生成条数。
     */
    public static final int DEFAULT_AUTO_QUESTION_COUNT = 3;

    /**
     * 自动问题生成上限。
     */
    public static final int MAX_AUTO_QUESTION_COUNT = 5;

    /**
     * 默认检索模式。
     */
    public static final String DEFAULT_SEARCH_MODE = "semantic";

    /**
     * 检索模式：语义。
     */
    public static final String SEARCH_MODE_SEMANTIC = "semantic";

    /**
     * 检索模式：全文。
     */
    public static final String SEARCH_MODE_FULLTEXT = "fulltext";

    /**
     * 检索模式：混合。
     */
    public static final String SEARCH_MODE_HYBRID = "hybrid";

    /**
     * 默认相似度阈值。
     */
    public static final BigDecimal DEFAULT_SIMILARITY_THRESHOLD = new BigDecimal("0.5");

    /**
     * 默认参考文本上限。
     */
    public static final Integer DEFAULT_REFERENCE_LIMIT = 4000;

    /**
     * 默认返回条数。
     */
    public static final Integer DEFAULT_TOP_K = 5;

    /**
     * 默认不启用Rerank。
     */
    public static final Boolean DEFAULT_USE_RERANK = Boolean.FALSE;

    /**
     * 默认Rerank候选数。
     */
    public static final int DEFAULT_RERANK_TOP_N = 20;

    /**
     * 默认不启用Query Optimization。
     */
    public static final Boolean DEFAULT_USE_QUERY_OPTIMIZATION = Boolean.FALSE;

    /**
     * 默认查询优化模式。
     */
    public static final String DEFAULT_QUERY_OPTIMIZATION_MODE = "rewrite";

    /**
     * 查询优化模式：关闭。
     */
    public static final String QUERY_OPTIMIZATION_MODE_OFF = "off";

    /**
     * 默认最多生成的优化query数量。
     */
    public static final int DEFAULT_MAX_REWRITE_QUERIES = 3;

    /**
     * 默认保留原始query。
     */
    public static final Boolean DEFAULT_KEEP_ORIGINAL_QUERY = Boolean.TRUE;

    /**
     * 查询优化query上限。
     */
    public static final int MAX_REWRITE_QUERIES_LIMIT = 10;

    /**
     * 命中来源：语义。
     */
    public static final String HIT_TYPE_SEMANTIC = "semantic";

    /**
     * 命中来源：全文。
     */
    public static final String HIT_TYPE_FULLTEXT = "fulltext";

    /**
     * 命中来源：混合。
     */
    public static final String HIT_TYPE_HYBRID = "hybrid";

    /**
     * 命中来源：外部。
     */
    public static final String HIT_TYPE_EXTERNAL = "external";

    /**
     * 来源范围：内部。
     */
    public static final String SOURCE_SCOPE_INTERNAL = "internal";

    /**
     * 来源范围：外部。
     */
    public static final String SOURCE_SCOPE_EXTERNAL = "external";

    /**
     * 检索日志状态：成功。
     */
    public static final String LOG_STATUS_SUCCESS = "success";

    /**
     * 检索日志状态：失败。
     */
    public static final String LOG_STATUS_FAILED = "failed";

    /**
     * 命中字段：索引文本。
     */
    public static final String MATCHED_FIELD_INDEX_TEXT = "index_text";

    /**
     * 命中字段：内容。
     */
    public static final String MATCHED_FIELD_CONTENT = "content";

    private KbConstants() {
    }
}
