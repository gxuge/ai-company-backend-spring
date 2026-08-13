package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserbrowsehistory.TsUserBrowseHistoryActionDto;
import org.jeecg.modules.system.dto.tsuserbrowsehistory.TsUserBrowseHistoryQueryDto;
import org.jeecg.modules.system.service.ITsUserBrowseHistoryService;
import org.jeecg.modules.system.vo.tsuserbrowsehistory.TsUserBrowseHistoryRecordVo;
import org.jeecg.modules.system.vo.tsuserbrowsehistory.TsUserBrowseHistoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户角色与故事浏览记录接口。
 */
@Tag(name = "TsUserBrowseHistory 用户浏览记录")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsUserBrowseHistoryController {

    @Autowired
    private ITsUserBrowseHistoryService tsUserBrowseHistoryService;

    /**
     * 分页查询当前用户浏览记录。
     *
     * @param request 查询参数
     * @return 浏览记录分页
     */
    @Operation(summary = "用户浏览记录分页查询")
    @GetMapping("/ts-user-browse-history")
    public Result<Page<TsUserBrowseHistoryVo>> listHistory(
            @Valid TsUserBrowseHistoryQueryDto request) {
        return tsUserBrowseHistoryService.pageHistory(currentUser(), request);
    }

    /**
     * 记录当前用户浏览在线公开角色或故事。
     *
     * @param request 资源参数
     * @return 更新后的浏览记录
     */
    @Operation(summary = "记录用户浏览行为")
    @PostMapping("/ts-user-browse-history")
    public Result<TsUserBrowseHistoryRecordVo> recordHistory(
            @Valid @RequestBody TsUserBrowseHistoryActionDto request) {
        return tsUserBrowseHistoryService.recordHistory(currentUser(), request);
    }

    /**
     * 删除当前用户指定浏览记录。
     *
     * @param request 资源参数
     * @return 删除结果
     */
    @Operation(summary = "删除用户浏览记录")
    @DeleteMapping("/ts-user-browse-history")
    public Result<?> removeHistory(@Valid TsUserBrowseHistoryActionDto request) {
        return tsUserBrowseHistoryService.deleteHistory(currentUser(), request);
    }

    /**
     * 清空当前用户全部浏览记录。
     *
     * @return 清空结果
     */
    @Operation(summary = "清空用户浏览记录")
    @DeleteMapping("/ts-user-browse-history/clear")
    public Result<?> clearHistory() {
        return tsUserBrowseHistoryService.clearHistory(currentUser());
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
