package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowActionDto;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowQueryDto;
import org.jeecg.modules.system.service.ITsUserFollowService;
import org.jeecg.modules.system.vo.tsuserfollow.TsUserFollowStatusVo;
import org.jeecg.modules.system.vo.tsuserfollow.TsUserFollowVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户关注接口。 */
@Tag(name = "TsUserFollow 用户关注")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-user-follows")
public class TsUserFollowController {

    private final ITsUserFollowService userFollowService;

    /** 注入用户关注服务。 */
    public TsUserFollowController(ITsUserFollowService userFollowService) {
        this.userFollowService = userFollowService;
    }

    /** 分页查询当前用户关注的用户。 */
    @Operation(summary = "分页查询我的关注")
    @GetMapping("/following")
    public Result<Page<TsUserFollowVo>> listFollowing(
            @Valid TsUserFollowQueryDto request) {
        return userFollowService.pageFollowing(currentUser(), request);
    }

    /** 分页查询当前用户的粉丝。 */
    @Operation(summary = "分页查询我的粉丝")
    @GetMapping("/followers")
    public Result<Page<TsUserFollowVo>> listFollowers(
            @Valid TsUserFollowQueryDto request) {
        return userFollowService.pageFollowers(currentUser(), request);
    }

    /** 查询当前用户对目标用户的关注状态。 */
    @Operation(summary = "查询用户关注状态")
    @GetMapping("/status")
    public Result<TsUserFollowStatusVo> getFollowStatus(
            @Valid TsUserFollowActionDto request) {
        return userFollowService.getFollowStatus(currentUser(), request);
    }

    /** 关注目标用户。 */
    @Operation(summary = "关注用户")
    @PostMapping
    public Result<TsUserFollowStatusVo> follow(
            @Valid @RequestBody TsUserFollowActionDto request) {
        return userFollowService.follow(currentUser(), request);
    }

    /** 取消关注目标用户。 */
    @Operation(summary = "取消关注用户")
    @DeleteMapping
    public Result<TsUserFollowStatusVo> unfollow(
            @Valid TsUserFollowActionDto request) {
        return userFollowService.unfollow(currentUser(), request);
    }

    /** 获取当前登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
