package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteActionDto;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteQueryDto;
import org.jeecg.modules.system.service.ITsUserFavoriteService;
import org.jeecg.modules.system.vo.tsuserfavorite.TsUserFavoriteStatusVo;
import org.jeecg.modules.system.vo.tsuserfavorite.TsUserFavoriteVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户角色与故事收藏接口。
 */
@Tag(name = "TsUserFavorite 用户收藏")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsUserFavoriteController {

    @Autowired
    private ITsUserFavoriteService tsUserFavoriteService;

    /**
     * 分页查询当前用户收藏。
     *
     * @param request 查询参数
     * @return 收藏分页
     */
    @Operation(summary = "用户收藏分页查询")
    @GetMapping("/ts-user-favorites")
    public Result<Page<TsUserFavoriteVo>> listFavorites(@Valid TsUserFavoriteQueryDto request) {
        return tsUserFavoriteService.pageFavorites(currentUser(), request);
    }

    /**
     * 查询当前用户对指定资源的收藏状态。
     *
     * @param request 资源参数
     * @return 收藏状态
     */
    @Operation(summary = "查询用户收藏状态")
    @GetMapping("/ts-user-favorites/status")
    public Result<TsUserFavoriteStatusVo> getFavoriteStatus(@Valid TsUserFavoriteActionDto request) {
        return tsUserFavoriteService.getFavoriteStatus(currentUser(), request);
    }

    /**
     * 收藏在线公开角色或故事。
     *
     * @param request 资源参数
     * @return 收藏状态
     */
    @Operation(summary = "新增用户收藏")
    @PostMapping("/ts-user-favorites")
    public Result<TsUserFavoriteStatusVo> createFavorite(
            @Valid @RequestBody TsUserFavoriteActionDto request) {
        return tsUserFavoriteService.addFavorite(currentUser(), request);
    }

    /**
     * 取消当前用户收藏。
     *
     * @param request 资源参数
     * @return 收藏状态
     */
    @Operation(summary = "取消用户收藏")
    @DeleteMapping("/ts-user-favorites")
    public Result<TsUserFavoriteStatusVo> removeFavorite(@Valid TsUserFavoriteActionDto request) {
        return tsUserFavoriteService.cancelFavorite(currentUser(), request);
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
