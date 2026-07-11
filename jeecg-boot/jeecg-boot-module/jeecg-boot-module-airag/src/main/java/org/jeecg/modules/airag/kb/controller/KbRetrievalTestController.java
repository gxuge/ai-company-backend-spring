package org.jeecg.modules.airag.kb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.KbRetrievalTestLogQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbSemanticSearchQueryDTO;
import org.jeecg.modules.airag.kb.service.IKbRetrievalTestService;
import org.jeecg.modules.airag.kb.vo.KbRetrievalTestLogVo;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库检索测试控制器。
 */
@Tag(name = "知识库检索测试")
@RestController
@RequestMapping("/kb/retrieval-test")
public class KbRetrievalTestController {
    /**
     * 检索测试服务。
     */
    private final IKbRetrievalTestService kbRetrievalTestService;

    /**
     * 构造方法。
     *
     * @param kbRetrievalTestService 检索测试服务
     */
    public KbRetrievalTestController(IKbRetrievalTestService kbRetrievalTestService) {
        this.kbRetrievalTestService = kbRetrievalTestService;
    }

    /**
     * 执行检索测试。
     *
     * @param kbId 知识库ID
     * @param dto 检索请求
     * @return 检索结果
     */
    @PostMapping("/{kbId}")
    @Operation(summary = "执行检索测试")
    public Result<KbSemanticSearchResultVO> testSearch(@PathVariable("kbId") String kbId, @Valid @RequestBody KbSemanticSearchQueryDTO dto) {
        return Result.OK(kbRetrievalTestService.testSearch(kbId, dto));
    }

    /**
     * 查询检索测试日志列表。
     *
     * @param dto 查询条件
     * @return 日志分页
     */
    @GetMapping("/logs")
    @Operation(summary = "查询检索测试日志列表")
    public Result<IPage<KbRetrievalTestLogVo>> pageLogs(@Valid KbRetrievalTestLogQueryDTO dto) {
        return Result.OK(kbRetrievalTestService.pageLogs(dto));
    }

    /**
     * 查询检索测试日志详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/logs/{id}")
    @Operation(summary = "查询检索测试日志详情")
    public Result<KbRetrievalTestLogVo> getLogById(@PathVariable("id") String id) {
        return Result.OK(kbRetrievalTestService.getLogById(id));
    }
}
