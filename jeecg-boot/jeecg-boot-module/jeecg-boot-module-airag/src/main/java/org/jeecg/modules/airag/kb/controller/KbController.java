package org.jeecg.modules.airag.kb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.*;
import org.jeecg.modules.airag.kb.service.IKbBaseService;
import org.jeecg.modules.airag.kb.service.IKbChunkService;
import org.jeecg.modules.airag.kb.service.IKbDocumentService;
import org.jeecg.modules.airag.kb.service.IKbSearchConfigService;
import org.jeecg.modules.airag.kb.vo.KbBaseVo;
import org.jeecg.modules.airag.kb.vo.KbChunkVo;
import org.jeecg.modules.airag.kb.vo.KbDocumentVo;
import org.jeecg.modules.airag.kb.vo.KbSearchConfigVo;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库基础结构控制器。
 */
@Tag(name = "知识库基础结构")
@RestController
@RequestMapping("/kb")
public class KbController {
    /**
     * 知识库主表服务。
     */
    private final IKbBaseService kbBaseService;

    /**
     * 文档服务。
     */
    private final IKbDocumentService kbDocumentService;

    /**
     * chunk服务。
     */
    private final IKbChunkService kbChunkService;

    /**
     * 搜索配置服务。
     */
    private final IKbSearchConfigService kbSearchConfigService;

    /**
     * 构造方法。
     *
     * @param kbBaseService 知识库主表服务
     * @param kbDocumentService 文档服务
     * @param kbChunkService chunk服务
     * @param kbSearchConfigService 搜索配置服务
     */
    public KbController(IKbBaseService kbBaseService,
                        IKbDocumentService kbDocumentService,
                        IKbChunkService kbChunkService,
                        IKbSearchConfigService kbSearchConfigService) {
        this.kbBaseService = kbBaseService;
        this.kbDocumentService = kbDocumentService;
        this.kbChunkService = kbChunkService;
        this.kbSearchConfigService = kbSearchConfigService;
    }

    /**
     * 创建知识库。
     *
     * @param dto 创建请求
     * @return 知识库详情
     */
    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    public Result<KbBaseVo> create(@Valid @RequestBody KbBaseCreateDto dto) {
        return Result.OK(kbBaseService.createKb(dto));
    }

    /**
     * 更新知识库。
     *
     * @param id 知识库ID
     * @param dto 更新请求
     * @return 知识库详情
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识库")
    public Result<KbBaseVo> update(@PathVariable("id") String id, @Valid @RequestBody KbBaseUpdateDto dto) {
        return Result.OK(kbBaseService.updateKb(id, dto));
    }

    /**
     * 删除知识库。
     *
     * @param id 知识库ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库")
    public Result<String> delete(@PathVariable("id") String id) {
        kbBaseService.deleteKb(id);
        return Result.OK("删除成功");
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库ID
     * @return 知识库详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询知识库详情")
    public Result<KbBaseVo> detail(@PathVariable("id") String id) {
        return Result.OK(kbBaseService.getKb(id));
    }

    /**
     * 分页查询知识库列表。
     *
     * @param query 查询请求
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询知识库列表")
    public Result<IPage<KbBaseVo>> list(@Valid KbBaseQueryDto query) {
        return Result.OK(kbBaseService.listKb(query));
    }

    /**
     * 在知识库下创建文档记录。
     *
     * @param kbId 知识库ID
     * @param dto 创建请求
     * @return 文档详情
     */
    @PostMapping("/{kbId}/documents")
    @Operation(summary = "创建文档记录")
    public Result<KbDocumentVo> createDocument(@PathVariable("kbId") String kbId, @Valid @RequestBody KbDocumentCreateDto dto) {
        return Result.OK(kbDocumentService.createDocument(kbId, dto));
    }

    /**
     * 查询知识库下的文档列表。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    @GetMapping("/{kbId}/documents")
    @Operation(summary = "查询知识库下的文档列表")
    public Result<IPage<KbDocumentVo>> listDocuments(@PathVariable("kbId") String kbId, @Valid KbDocumentQueryDto query) {
        return Result.OK(kbDocumentService.listDocuments(kbId, query));
    }

    /**
     * 删除文档记录。
     *
     * @param documentId 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "删除文档记录")
    public Result<String> deleteDocument(@PathVariable("documentId") String documentId) {
        kbDocumentService.deleteDocument(documentId);
        return Result.OK("删除成功");
    }

    /**
     * 在知识库下创建chunk。
     *
     * @param kbId 知识库ID
     * @param dto 创建请求
     * @return chunk详情
     */
    @PostMapping("/{kbId}/chunks")
    @Operation(summary = "创建chunk")
    public Result<KbChunkVo> createChunk(@PathVariable("kbId") String kbId, @Valid @RequestBody KbChunkCreateDto dto) {
        return Result.OK(kbChunkService.createChunk(kbId, dto));
    }

    /**
     * 更新chunk。
     *
     * @param chunkId chunkID
     * @param dto 更新请求
     * @return chunk详情
     */
    @PutMapping("/chunks/{chunkId}")
    @Operation(summary = "更新chunk")
    public Result<KbChunkVo> updateChunk(@PathVariable("chunkId") String chunkId, @Valid @RequestBody KbChunkUpdateDto dto) {
        return Result.OK(kbChunkService.updateChunk(chunkId, dto));
    }

    /**
     * 删除chunk。
     *
     * @param chunkId chunkID
     * @return 删除结果
     */
    @DeleteMapping("/chunks/{chunkId}")
    @Operation(summary = "删除chunk")
    public Result<String> deleteChunk(@PathVariable("chunkId") String chunkId) {
        kbChunkService.deleteChunk(chunkId);
        return Result.OK("删除成功");
    }

    /**
     * 查询知识库下的chunk列表。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    @GetMapping("/{kbId}/chunks")
    @Operation(summary = "查询知识库下的chunk列表")
    public Result<IPage<KbChunkVo>> listChunks(@PathVariable("kbId") String kbId, @Valid KbChunkQueryDto query) {
        return Result.OK(kbChunkService.listChunks(kbId, query));
    }

    /**
     * 保存知识库检索配置。
     *
     * @param kbId 知识库ID
     * @param dto 配置请求
     * @return 配置详情
     */
    @PostMapping("/{kbId}/search-config")
    @Operation(summary = "保存知识库检索配置")
    public Result<KbSearchConfigVo> saveSearchConfig(@PathVariable("kbId") String kbId, @Valid @RequestBody KbSearchConfigSaveDto dto) {
        return Result.OK(kbSearchConfigService.saveOrUpdateConfig(kbId, dto));
    }

    /**
     * 查询知识库检索配置。
     *
     * @param kbId 知识库ID
     * @return 配置详情
     */
    @GetMapping("/{kbId}/search-config")
    @Operation(summary = "查询知识库检索配置")
    public Result<KbSearchConfigVo> getSearchConfig(@PathVariable("kbId") String kbId) {
        return Result.OK(kbSearchConfigService.getByKbId(kbId));
    }
}
