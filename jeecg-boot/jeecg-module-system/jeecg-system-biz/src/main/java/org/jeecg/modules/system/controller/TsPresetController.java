package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.entity.TsPreset;
import org.jeecg.modules.system.service.ITsPresetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Description: 生成预设主表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Slf4j
@Tag(name = "生成预设主表")
@RestController
@RequestMapping("/sys/tsPreset")
public class TsPresetController {

    @Autowired
    private ITsPresetService tsPresetService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "生成预设主表-分页列表查询")
    @Operation(summary = "生成预设主表-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<TsPreset>> queryPageList(
            TsPreset tsPreset,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TsPreset> queryWrapper = QueryGenerator.initQueryWrapper(tsPreset, req.getParameterMap());
        Page<TsPreset> page = new Page<>(pageNo, pageSize);
        IPage<TsPreset> pageList = tsPresetService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 新增
     */
    @AutoLog(value = "生成预设主表-新增")
    @Operation(summary = "生成预设主表-新增")
    @PostMapping("/add")
    public Result<?> add(@RequestBody TsPreset tsPreset) {
        if (oConvertUtils.isEmpty(tsPreset.getName()) || oConvertUtils.isEmpty(tsPreset.getTargetType())) {
            return Result.error("name/targetType 不能为空");
        }
        tsPresetService.save(tsPreset);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "生成预设主表-编辑")
    @Operation(summary = "生成预设主表-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody TsPreset tsPreset) {
        if (oConvertUtils.isEmpty(tsPreset.getId())) {
            return Result.error("id 不能为空");
        }
        TsPreset entity = tsPresetService.getById(tsPreset.getId());
        if (entity == null) {
            return Result.error("未找到对应记录");
        }
        tsPresetService.updateById(tsPreset);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id查询
     */
    @AutoLog(value = "生成预设主表-通过id查询")
    @Operation(summary = "生成预设主表-通过id查询")
    @GetMapping("/queryById")
    public Result<TsPreset> queryById(@RequestParam(name = "id") String id) {
        TsPreset tsPreset = tsPresetService.getById(id);
        if (tsPreset == null) {
            return Result.error("未找到对应记录");
        }
        return Result.OK(tsPreset);
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "生成预设主表-通过id删除")
    @Operation(summary = "生成预设主表-通过id删除")
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        tsPresetService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "生成预设主表-批量删除")
    @Operation(summary = "生成预设主表-批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        if (oConvertUtils.isEmpty(ids)) {
            return Result.error("ids 不能为空");
        }
        tsPresetService.removeByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }
}
