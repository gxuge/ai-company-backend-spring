package org.jeecg.modules.airag.kb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.KbExternalKbQueryDto;
import org.jeecg.modules.airag.kb.dto.KbExternalKbSaveDto;
import org.jeecg.modules.airag.kb.service.IKbExternalKbService;
import org.jeecg.modules.airag.kb.vo.KbExternalKbVo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 外部知识库控制器。
 */
@Tag(name = "外部知识库")
@RestController
@RequestMapping("/kb/external")
public class KbExternalKbController {
    /**
     * 外部知识库服务。
     */
    private final IKbExternalKbService kbExternalKbService;

    /**
     * 构造方法。
     *
     * @param kbExternalKbService 外部知识库服务
     */
    public KbExternalKbController(IKbExternalKbService kbExternalKbService) {
        this.kbExternalKbService = kbExternalKbService;
    }

    /**
     * 新增。
     *
     * @param dto 请求
     * @return 结果
     */
    @PostMapping
    @Operation(summary = "新增外部知识库")
    public Result<KbExternalKbVo> create(@Valid @RequestBody KbExternalKbSaveDto dto) {
        return Result.OK(kbExternalKbService.create(dto));
    }

    /**
     * 更新。
     *
     * @param id 主键
     * @param dto 请求
     * @return 结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新外部知识库")
    public Result<KbExternalKbVo> update(@PathVariable("id") String id, @Valid @RequestBody KbExternalKbSaveDto dto) {
        return Result.OK(kbExternalKbService.update(id, dto));
    }

    /**
     * 删除。
     *
     * @param id 主键
     * @return 结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "禁用外部知识库")
    public Result<?> delete(@PathVariable("id") String id) {
        kbExternalKbService.delete(id);
        return Result.OK();
    }

    /**
     * 详情。
     *
     * @param id 主键
     * @return 结果
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询外部知识库详情")
    public Result<KbExternalKbVo> detail(@PathVariable("id") String id) {
        return Result.OK(kbExternalKbService.getDetail(id));
    }

    /**
     * 列表。
     *
     * @param dto 查询条件
     * @return 结果
     */
    @GetMapping("/list")
    @Operation(summary = "查询外部知识库列表")
    public Result<IPage<KbExternalKbVo>> list(@Valid KbExternalKbQueryDto dto) {
        return Result.OK(kbExternalKbService.page(dto));
    }

    /**
     * 测试连接。
     *
     * @param id 主键
     * @return 结果
     */
    @PostMapping("/{id}/test-connection")
    @Operation(summary = "测试外部知识库连接")
    public Result<String> testConnection(@PathVariable("id") String id) {
        return Result.OK(kbExternalKbService.testConnection(id));
    }
}
