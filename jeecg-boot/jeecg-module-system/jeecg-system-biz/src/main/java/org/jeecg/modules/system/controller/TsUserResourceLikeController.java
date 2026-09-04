package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeActionDto;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeQueryDto;
import org.jeecg.modules.system.service.ITsUserResourceLikeService;
import org.jeecg.modules.system.vo.tsuserresourcelike.TsUserResourceLikeStatusVo;
import org.jeecg.modules.system.vo.tsuserresourcelike.TsUserResourceLikeVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户角色与故事点赞接口。 */
@Tag(name = "TsUserResourceLike 用户资源点赞")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-user-resource-likes")
public class TsUserResourceLikeController {

    private final ITsUserResourceLikeService resourceLikeService;

    /** 注入用户资源点赞服务。 */
    public TsUserResourceLikeController(
            ITsUserResourceLikeService resourceLikeService) {
        this.resourceLikeService = resourceLikeService;
    }

    /** 分页查询当前用户点赞的角色和故事。 */
    @Operation(summary = "分页查询我的点赞")
    @GetMapping
    public Result<Page<TsUserResourceLikeVo>> listLikes(
            @Valid TsUserResourceLikeQueryDto request) {
        return resourceLikeService.pageLikes(currentUser(), request);
    }

    /** 查询当前用户对指定资源的点赞状态。 */
    @Operation(summary = "查询资源点赞状态")
    @GetMapping("/status")
    public Result<TsUserResourceLikeStatusVo> getLikeStatus(
            @Valid TsUserResourceLikeActionDto request) {
        return resourceLikeService.getLikeStatus(currentUser(), request);
    }

    /** 点赞在线公开角色或故事。 */
    @Operation(summary = "点赞角色或故事")
    @PostMapping
    public Result<TsUserResourceLikeStatusVo> like(
            @Valid @RequestBody TsUserResourceLikeActionDto request) {
        return resourceLikeService.like(currentUser(), request);
    }

    /** 取消当前用户对指定资源的点赞。 */
    @Operation(summary = "取消角色或故事点赞")
    @DeleteMapping
    public Result<TsUserResourceLikeStatusVo> unlike(
            @Valid TsUserResourceLikeActionDto request) {
        return resourceLikeService.unlike(currentUser(), request);
    }

    /** 获取当前登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
