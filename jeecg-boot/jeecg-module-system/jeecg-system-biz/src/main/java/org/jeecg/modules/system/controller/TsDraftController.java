package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsdraft.TsDraftQueryDto;
import org.jeecg.modules.system.dto.tsdraft.TsDraftSaveDto;
import org.jeecg.modules.system.service.ITsDraftService;
import org.jeecg.modules.system.vo.tsdraft.TsDraftDetailVo;
import org.jeecg.modules.system.vo.tsdraft.TsDraftListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色与故事统一草稿接口。
 */
@Tag(name = "TsDraft 统一草稿")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsDraftController {

    @Autowired
    private ITsDraftService tsDraftService;

    /**
     * 分页查询当前用户全部草稿。
     *
     * @param request 查询参数
     * @return 草稿分页，包含完整页面状态
     */
    @Operation(summary = "统一草稿分页查询")
    @GetMapping("/ts-drafts")
    public Result<Page<TsDraftListVo>> listDrafts(@Valid TsDraftQueryDto request) {
        return tsDraftService.pageDrafts(currentUser(), request);
    }

    /**
     * 查询当前用户单条草稿详情。
     *
     * @param id 草稿 ID
     * @return 草稿详情
     */
    @Operation(summary = "统一草稿详情")
    @GetMapping("/ts-drafts/detail")
    public Result<TsDraftDetailVo> getDraft(@RequestParam("id") Long id) {
        return tsDraftService.getDraft(currentUser(), id);
    }

    /**
     * 新增当前用户草稿。
     *
     * @param request 保存参数
     * @return 新增后的草稿详情
     */
    @Operation(summary = "新增统一草稿")
    @PostMapping("/ts-drafts")
    public Result<TsDraftDetailVo> createDraft(
            @Validated(TsDraftSaveDto.Create.class) @RequestBody TsDraftSaveDto request) {
        return tsDraftService.addDraft(currentUser(), request);
    }

    /**
     * 编辑当前用户草稿。
     *
     * @param request 保存参数
     * @return 编辑后的草稿详情
     */
    @Operation(summary = "编辑统一草稿")
    @PutMapping("/ts-drafts")
    public Result<TsDraftDetailVo> updateDraft(
            @Validated(TsDraftSaveDto.Update.class) @RequestBody TsDraftSaveDto request) {
        return tsDraftService.editDraft(currentUser(), request.getId(), request);
    }

    /**
     * 删除当前用户草稿。
     *
     * @param id 草稿 ID
     * @return 删除结果
     */
    @Operation(summary = "删除统一草稿")
    @DeleteMapping("/ts-drafts")
    public Result<?> removeDraft(@RequestParam("id") Long id) {
        return tsDraftService.deleteDraft(currentUser(), id);
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
