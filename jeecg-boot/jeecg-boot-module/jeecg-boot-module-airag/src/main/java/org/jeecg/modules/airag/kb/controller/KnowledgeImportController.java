package org.jeecg.modules.airag.kb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.ImportConfirmDTO;
import org.jeecg.modules.airag.kb.dto.ImportTextDTO;
import org.jeecg.modules.airag.kb.service.KnowledgeImportService;
import org.jeecg.modules.airag.kb.vo.ChunkPreviewVO;
import org.jeecg.modules.airag.kb.vo.KnowledgeImportResultVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库导入控制器。
 */
@Tag(name = "知识库导入")
@RestController
@RequestMapping("/kb")
public class KnowledgeImportController {
    /**
     * 知识库导入服务。
     */
    private final KnowledgeImportService knowledgeImportService;

    /**
     * 构造方法。
     *
     * @param knowledgeImportService 知识库导入服务
     */
    public KnowledgeImportController(KnowledgeImportService knowledgeImportService) {
        this.knowledgeImportService = knowledgeImportService;
    }

    /**
     * 手动文本导入。
     *
     * @param kbId 知识库ID
     * @param dto 导入请求
     * @return 导入结果
     */
    @PostMapping("/{kbId}/import/text")
    @Operation(summary = "手动文本导入")
    public Result<KnowledgeImportResultVO> importText(@PathVariable("kbId") String kbId, @Valid @RequestBody ImportTextDTO dto) {
        return Result.OK(knowledgeImportService.importText(kbId, dto));
    }

    /**
     * 文件上传导入。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @param chunkSize 分段长度
     * @param chunkOverlap 分段重叠长度
     * @return 导入结果
     */
    @PostMapping(value = "/{kbId}/import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "文件上传导入")
    public Result<KnowledgeImportResultVO> importFile(@PathVariable("kbId") String kbId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "chunk_size", required = false) Integer chunkSize,
                                                      @RequestParam(value = "chunk_overlap", required = false) Integer chunkOverlap) {
        return Result.OK(knowledgeImportService.importFile(kbId, file, chunkSize, chunkOverlap));
    }

    /**
     * 文本切分预览。
     *
     * @param kbId 知识库ID
     * @param dto 导入请求
     * @return chunk预览列表
     */
    @PostMapping("/{kbId}/chunks/preview-text")
    @Operation(summary = "文本切分预览")
    public Result<List<ChunkPreviewVO>> previewText(@PathVariable("kbId") String kbId, @Valid @RequestBody ImportTextDTO dto) {
        return Result.OK(knowledgeImportService.previewText(kbId, dto));
    }

    /**
     * 文件切分预览。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @param chunkSize 分段长度
     * @param chunkOverlap 分段重叠长度
     * @return chunk预览列表
     */
    @PostMapping(value = "/{kbId}/chunks/preview-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "文件切分预览")
    public Result<List<ChunkPreviewVO>> previewFile(@PathVariable("kbId") String kbId,
                                                    @RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "chunk_size", required = false) Integer chunkSize,
                                                    @RequestParam(value = "chunk_overlap", required = false) Integer chunkOverlap) {
        return Result.OK(knowledgeImportService.previewFile(kbId, file, chunkSize, chunkOverlap));
    }

    /**
     * 基于预览结果确认导入。
     *
     * @param kbId 知识库ID
     * @param dto 确认请求
     * @return 导入结果
     */
    @PostMapping("/{kbId}/import/confirm")
    @Operation(summary = "基于预览结果确认导入")
    public Result<KnowledgeImportResultVO> confirmImport(@PathVariable("kbId") String kbId, @Valid @RequestBody ImportConfirmDTO dto) {
        return Result.OK(knowledgeImportService.confirmImport(kbId, dto));
    }
}
