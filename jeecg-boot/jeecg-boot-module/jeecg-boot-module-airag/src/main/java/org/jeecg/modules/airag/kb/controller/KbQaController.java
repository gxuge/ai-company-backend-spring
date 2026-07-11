package org.jeecg.modules.airag.kb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.KbAutoQuestionDTO;
import org.jeecg.modules.airag.kb.dto.KbQaBatchDTO;
import org.jeecg.modules.airag.kb.dto.KbQaImportConfirmDTO;
import org.jeecg.modules.airag.kb.dto.KbQaItemDTO;
import org.jeecg.modules.airag.kb.service.IKbQaService;
import org.jeecg.modules.airag.kb.vo.KbAutoQuestionPreviewVO;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;
import org.jeecg.modules.airag.kb.vo.KbQaImportPreviewVO;
import org.jeecg.modules.airag.kb.vo.KbQaImportResultVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * QA导入与多索引控制器。
 */
@Tag(name = "QA导入与多索引")
@RestController
@RequestMapping("/kb")
public class KbQaController {
    /**
     * QA服务。
     */
    private final IKbQaService kbQaService;

    /**
     * 构造方法。
     *
     * @param kbQaService QA服务
     */
    public KbQaController(IKbQaService kbQaService) {
        this.kbQaService = kbQaService;
    }

    /**
     * 手动新增单条QA。
     *
     * @param kbId 知识库ID
     * @param dto QA条目
     * @return 导入结果
     */
    @PostMapping("/{kbId}/qa")
    @Operation(summary = "手动新增单条QA")
    public Result<KbQaImportResultVO> createQa(@PathVariable("kbId") String kbId,
                                               @Valid @RequestBody KbQaItemDTO dto) {
        return Result.OK(kbQaService.createQa(kbId, dto));
    }

    /**
     * 批量新增QA。
     *
     * @param kbId 知识库ID
     * @param dto 批量请求
     * @return 导入结果
     */
    @PostMapping("/{kbId}/qa/batch")
    @Operation(summary = "批量新增QA")
    public Result<KbQaImportResultVO> batchCreateQa(@PathVariable("kbId") String kbId,
                                                    @Valid
                                                    @RequestBody KbQaBatchDTO dto) {
        return Result.OK(kbQaService.batchCreateQa(kbId, dto));
    }

    /**
     * CSV/Excel导入预览。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @return 预览结果
     */
    @PostMapping(value = "/{kbId}/qa/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "CSV/Excel导入预览")
    public Result<KbQaImportPreviewVO> previewImport(@PathVariable("kbId") String kbId,
                                                     @RequestPart("file") MultipartFile file) {
        return Result.OK(kbQaService.previewImport(kbId, file));
    }

    /**
     * CSV/Excel确认导入。
     *
     * @param kbId 知识库ID
     * @param dto 确认请求
     * @return 导入结果
     */
    @PostMapping("/{kbId}/qa/import/confirm")
    @Operation(summary = "CSV/Excel确认导入")
    public Result<KbQaImportResultVO> confirmImport(@PathVariable("kbId") String kbId,
                                                    @Valid @RequestBody KbQaImportConfirmDTO dto) {
        return Result.OK(kbQaService.confirmImport(kbId, dto));
    }

    /**
     * 基于chunk预览自动生成索引问题。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dto 请求
     * @return 预览结果
     */
    @PostMapping("/{kbId}/chunks/{chunkId}/auto-questions/preview")
    @Operation(summary = "基于chunk预览自动生成索引问题")
    public Result<KbAutoQuestionPreviewVO> previewAutoQuestionsByChunk(@PathVariable("kbId") String kbId,
                                                                       @PathVariable("chunkId") String chunkId,
                                                                       @RequestBody(required = false) KbAutoQuestionDTO dto) {
        return Result.OK(kbQaService.previewAutoQuestions(kbId, chunkId, dto));
    }

    /**
     * 基于文本预览自动生成索引问题。
     *
     * @param kbId 知识库ID
     * @param dto 请求
     * @return 预览结果
     */
    @PostMapping("/{kbId}/auto-questions/preview")
    @Operation(summary = "基于文本预览自动生成索引问题")
    public Result<KbAutoQuestionPreviewVO> previewAutoQuestionsByContent(@PathVariable("kbId") String kbId,
                                                                         @RequestBody KbAutoQuestionDTO dto) {
        return Result.OK(kbQaService.previewAutoQuestions(kbId, dto));
    }

    /**
     * 基于chunk保存自动生成的索引问题。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dto 请求
     * @return 创建的索引列表
     */
    @PostMapping("/{kbId}/chunks/{chunkId}/auto-questions")
    @Operation(summary = "基于chunk保存自动生成的索引问题")
    public Result<List<KbChunkIndexVo>> saveAutoQuestions(@PathVariable("kbId") String kbId,
                                                          @PathVariable("chunkId") String chunkId,
                                                          @RequestBody(required = false) KbAutoQuestionDTO dto) {
        return Result.OK(kbQaService.saveAutoQuestions(kbId, chunkId, dto));
    }
}
