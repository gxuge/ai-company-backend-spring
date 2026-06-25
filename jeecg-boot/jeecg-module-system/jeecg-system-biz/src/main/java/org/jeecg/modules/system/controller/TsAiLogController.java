package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.system.entity.TsAiLog;
import org.jeecg.modules.system.service.ITsAiLogService;
import org.jeecg.modules.system.vo.tsailog.TsAiLogDetailVo;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "ts AI调用监控")
@RestController
@RequestMapping("/sys/tsAiLog")
public class TsAiLogController {

    @Autowired
    private ITsAiLogService tsAiLogService;

    @AutoLog(value = "ts AI调用监控-分页列表查询")
    @Operation(summary = "ts AI调用监控-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<TsAiLog>> queryPageList(
            TsAiLog tsAiLog,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        String endpointKeyword = tsAiLog != null ? tsAiLog.getEndpoint() : null;
        if (tsAiLog != null) {
            tsAiLog.setEndpoint(null);
        }
        QueryWrapper<TsAiLog> queryWrapper = QueryGenerator.initQueryWrapper(tsAiLog, req.getParameterMap());
        queryWrapper.like(StringUtils.hasText(endpointKeyword), "endpoint", endpointKeyword);
        queryWrapper.orderByDesc("create_time");
        Page<TsAiLog> page = new Page<>(pageNo, pageSize);
        IPage<TsAiLog> pageList = tsAiLogService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "ts AI调用监控-通过id查询")
    @Operation(summary = "ts AI调用监控-通过id查询")
    @GetMapping("/queryById")
    public Result<TsAiLog> queryById(@RequestParam(name = "id") Long id) {
        TsAiLog logEntity = tsAiLogService.getById(id);
        if (logEntity == null) {
            return Result.error("未找到对应记录");
        }
        return Result.OK(logEntity);
    }

    @AutoLog(value = "ts AI调用监控-详情查询")
    @Operation(summary = "ts AI调用监控-详情查询")
    @GetMapping("/detail")
    public Result<TsAiLogDetailVo> detail(@RequestParam(name = "id") Long id) {
        TsAiLogDetailVo detail = tsAiLogService.getDetail(id);
        if (detail.getLog() == null) {
            return Result.error("未找到对应记录");
        }
        return Result.OK(detail);
    }
}
