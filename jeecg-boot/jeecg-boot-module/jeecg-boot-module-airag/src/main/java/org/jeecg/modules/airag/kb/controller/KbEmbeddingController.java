package org.jeecg.modules.airag.kb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.service.KbEmbeddingService;
import org.jeecg.modules.airag.kb.vo.EmbeddingBatchResultVO;
import org.jeecg.modules.airag.kb.vo.EmbeddingStatusVO;
import org.springframework.web.bind.annotation.*;

/**
 * KB embedding控制器。
 */
@Tag(name = "KB Embedding")
@RestController
@RequestMapping("/kb")
public class KbEmbeddingController {
    /**
     * embedding编排服务。
     */
    private final KbEmbeddingService kbEmbeddingService;

    /**
     * 构造方法。
     *
     * @param kbEmbeddingService embedding编排服务
     */
    public KbEmbeddingController(KbEmbeddingService kbEmbeddingService) {
        this.kbEmbeddingService = kbEmbeddingService;
    }

    /**
     * 对整个知识库执行embedding。
     *
     * @param kbId 知识库ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @PostMapping("/{kbId}/embedding")
    @Operation(summary = "对整个知识库执行embedding")
    public Result<EmbeddingBatchResultVO> embedKb(@PathVariable("kbId") String kbId,
                                                  @RequestParam(value = "overrideSuccess", required = false, defaultValue = "false") boolean overrideSuccess) {
        return Result.OK(kbEmbeddingService.embedKb(kbId, overrideSuccess));
    }

    /**
     * 对单个文档执行embedding。
     *
     * @param documentId 文档ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @PostMapping("/documents/{documentId}/embedding")
    @Operation(summary = "对单个文档执行embedding")
    public Result<EmbeddingBatchResultVO> embedDocument(@PathVariable("documentId") String documentId,
                                                        @RequestParam(value = "overrideSuccess", required = false, defaultValue = "false") boolean overrideSuccess) {
        return Result.OK(kbEmbeddingService.embedDocument(documentId, overrideSuccess));
    }

    /**
     * 对单个chunk执行embedding。
     *
     * @param chunkId chunk ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @PostMapping("/chunks/{chunkId}/embedding")
    @Operation(summary = "对单个chunk执行embedding")
    public Result<EmbeddingBatchResultVO> embedChunk(@PathVariable("chunkId") String chunkId,
                                                     @RequestParam(value = "overrideSuccess", required = false, defaultValue = "false") boolean overrideSuccess) {
        return Result.OK(kbEmbeddingService.embedChunk(chunkId, overrideSuccess));
    }

    /**
     * 对单个chunk_index执行embedding。
     *
     * @param indexId chunk_index ID
     * @param overrideSuccess 是否覆盖成功数据
     * @return 处理结果
     */
    @PostMapping("/chunk-index/{indexId}/embedding")
    @Operation(summary = "对单个chunk_index执行embedding")
    public Result<EmbeddingBatchResultVO> embedIndex(@PathVariable("indexId") String indexId,
                                                     @RequestParam(value = "overrideSuccess", required = false, defaultValue = "true") boolean overrideSuccess) {
        return Result.OK(kbEmbeddingService.embedIndex(indexId, overrideSuccess));
    }

    /**
     * 查询知识库embedding进度。
     *
     * @param kbId 知识库ID
     * @return 统计结果
     */
    @GetMapping("/{kbId}/embedding/status")
    @Operation(summary = "查询知识库embedding进度")
    public Result<EmbeddingStatusVO> getStatus(@PathVariable("kbId") String kbId) {
        return Result.OK(kbEmbeddingService.getStatus(kbId));
    }
}
