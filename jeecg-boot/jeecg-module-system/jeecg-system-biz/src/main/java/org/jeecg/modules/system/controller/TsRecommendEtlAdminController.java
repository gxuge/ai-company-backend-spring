package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlExecutionQueryDto;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlIdDto;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlTaskQueryDto;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlTaskSaveDto;
import org.jeecg.modules.system.dto.recommendetl.TsRecommendEtlToggleDto;
import org.jeecg.modules.system.service.ITsRecommendEtlExecutionService;
import org.jeecg.modules.system.service.ITsRecommendEtlTaskService;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlExecutionVo;
import org.jeecg.modules.system.vo.recommendetl.TsRecommendEtlTaskVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 推荐训练数据 ETL 后台管理接口。 */
@Tag(name = "TsRecommendEtlAdmin 推荐训练数据 ETL")
@RestController
@Validated
@RequiresRoles("admin")
@RequestMapping("/sys/ts-recommend-etl")
public class TsRecommendEtlAdminController {
    private final ITsRecommendEtlTaskService taskService;
    private final ITsRecommendEtlExecutionService executionService;

    /** 注入任务和执行管理服务。 */
    public TsRecommendEtlAdminController(
            ITsRecommendEtlTaskService taskService,
            ITsRecommendEtlExecutionService executionService) {
        this.taskService = taskService;
        this.executionService = executionService;
    }

    /** 分页查询 ETL 任务。 */
    @Operation(summary = "分页查询 ETL 任务")
    @PostMapping("/task/page")
    public Result<Page<TsRecommendEtlTaskVo>> pageTasks(
            @RequestBody TsRecommendEtlTaskQueryDto request) {
        return Result.OK(taskService.pageTasks(request));
    }

    /** 查询 ETL 任务详情。 */
    @Operation(summary = "查询 ETL 任务详情")
    @GetMapping("/task/detail")
    public Result<TsRecommendEtlTaskVo> getTask(@RequestParam("id") Long id) {
        return Result.OK(taskService.getTask(id));
    }

    /** 新增 ETL 任务。 */
    @Operation(summary = "新增 ETL 任务")
    @PostMapping("/task/create")
    public Result<TsRecommendEtlTaskVo> createTask(
            @Validated @RequestBody TsRecommendEtlTaskSaveDto request) {
        return Result.OK(taskService.createTask(request));
    }

    /** 更新 ETL 任务。 */
    @Operation(summary = "更新 ETL 任务")
    @PostMapping("/task/update")
    public Result<TsRecommendEtlTaskVo> updateTask(
            @Validated @RequestBody TsRecommendEtlTaskSaveDto request) {
        return Result.OK(taskService.updateTask(request));
    }

    /** 删除未运行的 ETL 任务。 */
    @Operation(summary = "删除 ETL 任务")
    @PostMapping("/task/delete")
    public Result<String> deleteTask(
            @Validated @RequestBody TsRecommendEtlIdDto request) {
        taskService.deleteTask(request.getId());
        return Result.OK("删除成功");
    }

    /** 启用或停用 ETL 任务。 */
    @Operation(summary = "启用或停用 ETL 任务")
    @PostMapping("/task/toggle")
    public Result<TsRecommendEtlTaskVo> toggleTask(
            @Validated @RequestBody TsRecommendEtlToggleDto request) {
        return Result.OK(taskService.toggleTask(
                request.getId(), request.getEnabled()));
    }

    /** 手动执行 ETL 任务。 */
    @Operation(summary = "手动执行 ETL 任务")
    @PostMapping("/task/execute")
    public Result<TsRecommendEtlExecutionVo> executeTask(
            @Validated @RequestBody TsRecommendEtlIdDto request) {
        return Result.OK(executionService.triggerManual(request.getId()));
    }

    /** 分页查询 ETL 执行记录。 */
    @Operation(summary = "分页查询 ETL 执行记录")
    @PostMapping("/execution/page")
    public Result<Page<TsRecommendEtlExecutionVo>> pageExecutions(
            @RequestBody TsRecommendEtlExecutionQueryDto request) {
        return Result.OK(executionService.pageExecutions(request));
    }

    /** 查询 ETL 执行详情。 */
    @Operation(summary = "查询 ETL 执行详情")
    @GetMapping("/execution/detail")
    public Result<TsRecommendEtlExecutionVo> getExecution(
            @RequestParam("id") Long id) {
        return Result.OK(executionService.getExecution(id));
    }
}
