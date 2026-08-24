package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardUserQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskListQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskReceiveDto;
import org.jeecg.modules.system.service.ITsActivityService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityHomeVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardRecordVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivitySignVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityTaskVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前用户活动中心接口。 */
@Tag(name = "TsActivity 用户活动中心")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-activity")
public class TsActivityController {

    private final ITsActivityService activityService;

    /** 注入活动中心服务。 */
    public TsActivityController(ITsActivityService activityService) {
        this.activityService = activityService;
    }

    /** 查询活动首页。 */
    @Operation(summary = "查询活动首页")
    @GetMapping("/home")
    public Result<TsActivityHomeVo> getHome() {
        return Result.OK(activityService.getHome(currentUser().getId()));
    }

    /** 执行每日签到。 */
    @Operation(summary = "每日签到")
    @PostMapping("/sign")
    public Result<TsActivitySignVo> sign() {
        return Result.OK(activityService.sign(currentUser().getId()));
    }

    /** 查询当前用户活动任务。 */
    @Operation(summary = "查询活动任务")
    @GetMapping("/tasks")
    public Result<List<TsActivityTaskVo>> listTasks(
            TsActivityTaskListQueryDto request) {
        return Result.OK(activityService.listTasks(currentUser().getId(), request));
    }

    /** 领取任务奖励，任务ID通过JSON Body传递。 */
    @Operation(summary = "领取任务奖励")
    @PostMapping("/task/receive")
    public Result<TsActivityRewardGrantVo> receiveTaskReward(
            @Validated @RequestBody TsActivityTaskReceiveDto request) {
        return Result.OK(activityService.receiveTaskReward(
                currentUser().getId(), request));
    }

    /** 分页查询当前用户奖励记录。 */
    @Operation(summary = "查询奖励记录")
    @GetMapping("/rewards")
    public Result<Page<TsActivityRewardRecordVo>> pageRewards(
            TsActivityRewardUserQueryDto request) {
        return Result.OK(activityService.pageRewards(currentUser().getId(), request));
    }

    /** 获取当前登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
