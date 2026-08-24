package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardRuleSaveDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskCreateDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskUpdateDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityUserTaskQueryDto;
import org.jeecg.modules.system.entity.TsActivityTaskRewardRule;
import org.jeecg.modules.system.service.ITsActivityAdminService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminUserTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardRecordVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 活动中心后台管理接口。 */
@Tag(name = "TsActivityAdmin 活动后台管理")
@RestController
@Validated
@RequiresRoles("admin")
@RequestMapping("/sys/ts-activity-admin")
public class TsActivityAdminController {

    private final ITsActivityAdminService activityAdminService;

    /** 注入活动后台管理服务。 */
    public TsActivityAdminController(ITsActivityAdminService activityAdminService) {
        this.activityAdminService = activityAdminService;
    }

    /** 分页查询活动任务。 */
    @Operation(summary = "分页查询活动任务")
    @PostMapping("/task/page")
    public Result<Page<TsActivityAdminTaskVo>> pageTasks(
            @RequestBody TsActivityTaskQueryDto request) {
        return Result.OK(activityAdminService.pageTasks(request));
    }

    /** 创建活动任务。 */
    @Operation(summary = "创建活动任务")
    @PostMapping("/task/create")
    public Result<Long> createTask(
            @Validated @RequestBody TsActivityTaskCreateDto request) {
        return Result.OK(activityAdminService.createTask(request));
    }

    /** 编辑活动任务，任务ID通过JSON Body传递。 */
    @Operation(summary = "编辑活动任务")
    @PostMapping("/task/update")
    public Result<Void> updateTask(
            @Validated @RequestBody TsActivityTaskUpdateDto request) {
        activityAdminService.updateTask(request);
        return Result.OK("保存成功");
    }

    /** 分页查询用户任务进度。 */
    @Operation(summary = "分页查询用户任务进度")
    @PostMapping("/user-task/page")
    public Result<Page<TsActivityAdminUserTaskVo>> pageUserTasks(
            @RequestBody TsActivityUserTaskQueryDto request) {
        return Result.OK(activityAdminService.pageUserTasks(request));
    }

    /** 分页查询活动奖励记录。 */
    @Operation(summary = "分页查询活动奖励记录")
    @PostMapping("/reward/page")
    public Result<Page<TsActivityRewardRecordVo>> pageRewards(
            @RequestBody TsActivityRewardQueryDto request) {
        return Result.OK(activityAdminService.pageRewards(request));
    }

    /** 查询会员奖励加成规则。 */
    @Operation(summary = "查询会员奖励加成规则")
    @GetMapping("/reward-rule/list")
    public Result<List<TsActivityTaskRewardRule>> listRewardRules() {
        return Result.OK(activityAdminService.listRewardRules());
    }

    /** 保存会员奖励加成规则。 */
    @Operation(summary = "保存会员奖励加成规则")
    @PostMapping("/reward-rule/save")
    public Result<Void> saveRewardRule(
            @Validated @RequestBody TsActivityRewardRuleSaveDto request) {
        activityAdminService.saveRewardRule(request);
        return Result.OK("保存成功");
    }
}
