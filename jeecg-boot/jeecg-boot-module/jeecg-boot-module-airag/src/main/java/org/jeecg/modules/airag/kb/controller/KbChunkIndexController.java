package org.jeecg.modules.airag.kb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexQueryDto;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexSaveDto;
import org.jeecg.modules.airag.kb.service.IKbChunkIndexService;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * chunk索引控制器。
 */
@Tag(name = "chunk索引")
@RestController
@RequestMapping("/kb")
public class KbChunkIndexController {
    /**
     * chunk索引服务。
     */
    private final IKbChunkIndexService kbChunkIndexService;

    /**
     * 构造方法。
     *
     * @param kbChunkIndexService chunk索引服务
     */
    public KbChunkIndexController(IKbChunkIndexService kbChunkIndexService) {
        this.kbChunkIndexService = kbChunkIndexService;
    }

    /**
     * 创建chunk索引。
     *
     * @param kbId 知识库ID
     * @param chunkId 分段ID
     * @param dto 保存请求
     * @return 返回对象
     */
    @PostMapping("/{kbId}/chunks/{chunkId}/indexes")
    @Operation(summary = "创建chunk索引")
    public Result<KbChunkIndexVo> createIndex(@PathVariable("kbId") String kbId,
                                              @PathVariable("chunkId") String chunkId,
                                              @Valid @RequestBody KbChunkIndexSaveDto dto) {
        return Result.OK(kbChunkIndexService.createIndex(kbId, chunkId, dto));
    }

    /**
     * 批量创建chunk索引。
     *
     * @param kbId 知识库ID
     * @param chunkId 分段ID
     * @param dtoList 保存请求列表
     * @return 返回对象列表
     */
    @PostMapping("/{kbId}/chunks/{chunkId}/indexes/batch")
    @Operation(summary = "批量创建chunk索引")
    public Result<List<KbChunkIndexVo>> createIndexes(@PathVariable("kbId") String kbId,
                                                      @PathVariable("chunkId") String chunkId,
                                                      @RequestBody List<KbChunkIndexSaveDto> dtoList) {
        return Result.OK(kbChunkIndexService.createIndexes(kbId, chunkId, dtoList));
    }

    /**
     * 更新chunk索引。
     *
     * @param indexId 索引ID
     * @param dto 保存请求
     * @return 返回对象
     */
    @PutMapping("/indexes/{indexId}")
    @Operation(summary = "更新chunk索引")
    public Result<KbChunkIndexVo> updateIndex(@PathVariable("indexId") String indexId,
                                              @Valid @RequestBody KbChunkIndexSaveDto dto) {
        return Result.OK(kbChunkIndexService.updateIndex(indexId, dto));
    }

    /**
     * 删除chunk索引。
     *
     * @param indexId 索引ID
     * @return 删除结果
     */
    @DeleteMapping("/indexes/{indexId}")
    @Operation(summary = "删除chunk索引")
    public Result<String> deleteIndex(@PathVariable("indexId") String indexId) {
        kbChunkIndexService.deleteIndex(indexId);
        return Result.OK("删除成功");
    }

    /**
     * 查询chunk索引详情。
     *
     * @param indexId 索引ID
     * @return 返回对象
     */
    @GetMapping("/indexes/{indexId}")
    @Operation(summary = "查询chunk索引详情")
    public Result<KbChunkIndexVo> detail(@PathVariable("indexId") String indexId) {
        return Result.OK(kbChunkIndexService.getIndex(indexId));
    }

    /**
     * 分页查询chunk索引列表。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    @GetMapping("/{kbId}/indexes")
    @Operation(summary = "分页查询chunk索引列表")
    public Result<IPage<KbChunkIndexVo>> list(@PathVariable("kbId") String kbId, @Valid KbChunkIndexQueryDto query) {
        return Result.OK(kbChunkIndexService.listIndexes(kbId, query));
    }

    /**
     * 查询chunk下的索引列表。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param query 查询请求
     * @return 分页结果
     */
    @GetMapping("/{kbId}/chunks/{chunkId}/indexes")
    @Operation(summary = "查询chunk下的索引列表")
    public Result<IPage<KbChunkIndexVo>> listByChunk(@PathVariable("kbId") String kbId,
                                                     @PathVariable("chunkId") String chunkId,
                                                     @Valid KbChunkIndexQueryDto query) {
        if (query == null) {
            query = new KbChunkIndexQueryDto();
        }
        query.setChunkId(chunkId);
        return Result.OK(kbChunkIndexService.listIndexes(kbId, query));
    }
}
