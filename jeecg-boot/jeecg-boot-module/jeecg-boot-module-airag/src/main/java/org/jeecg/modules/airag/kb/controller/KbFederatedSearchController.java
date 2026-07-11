package org.jeecg.modules.airag.kb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.KbFederatedSearchQueryDTO;
import org.jeecg.modules.airag.kb.service.IKbFederatedSearchService;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多知识库联邦检索控制器。
 */
@Tag(name = "多知识库联邦检索")
@RestController
@RequestMapping("/kb/federated")
public class KbFederatedSearchController {
    /**
     * 联邦检索服务。
     */
    private final IKbFederatedSearchService kbFederatedSearchService;

    /**
     * 构造方法。
     *
     * @param kbFederatedSearchService 联邦检索服务
     */
    public KbFederatedSearchController(IKbFederatedSearchService kbFederatedSearchService) {
        this.kbFederatedSearchService = kbFederatedSearchService;
    }

    /**
     * 执行联邦检索。
     *
     * @param dto 请求
     * @return 结果
     */
    @PostMapping("/search")
    @Operation(summary = "多知识库联邦检索")
    public Result<KbSemanticSearchResultVO> search(@Valid @RequestBody KbFederatedSearchQueryDTO dto) {
        return Result.OK(kbFederatedSearchService.search(dto));
    }
}
