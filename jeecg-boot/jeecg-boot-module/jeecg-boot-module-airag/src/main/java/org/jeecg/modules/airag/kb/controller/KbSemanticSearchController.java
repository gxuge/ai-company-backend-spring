package org.jeecg.modules.airag.kb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.KbSemanticSearchQueryDTO;
import org.jeecg.modules.airag.kb.service.IKbSemanticSearchService;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库语义检索控制器。
 */
@Tag(name = "知识库语义检索")
@RestController
@RequestMapping("/kb")
public class KbSemanticSearchController {
    /**
     * 语义检索服务。
     */
    private final IKbSemanticSearchService kbSemanticSearchService;

    /**
     * 构造方法。
     *
     * @param kbSemanticSearchService 语义检索服务
     */
    public KbSemanticSearchController(IKbSemanticSearchService kbSemanticSearchService) {
        this.kbSemanticSearchService = kbSemanticSearchService;
    }

    /**
     * 执行语义检索。
     *
     * @param kbId 知识库ID
     * @param dto 检索请求
     * @return 检索结果
     */
    @PostMapping("/{kbId}/search")
    @Operation(summary = "知识库语义检索")
    public Result<KbSemanticSearchResultVO> search(@PathVariable("kbId") String kbId, @Valid @RequestBody KbSemanticSearchQueryDTO dto) {
        try {
            return Result.OK(kbSemanticSearchService.search(kbId, dto));
        } finally {
            kbSemanticSearchService.consumeLastSearchTrace();
        }
    }
}
