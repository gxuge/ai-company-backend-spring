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
import org.jeecg.modules.system.entity.TsPresetTag;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.service.ITsPresetTagService;
import org.jeecg.modules.system.service.ITsTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Description: 预设与标签关联表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Slf4j
@Tag(name = "预设与标签关联表")
@RestController
@RequestMapping("/sys/tsPresetTag")
public class TsPresetTagController {

    @Autowired
    private ITsPresetTagService tsPresetTagService;
    @Autowired
    private ITsTagService tsTagService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "预设与标签关联表-分页列表查询")
    @Operation(summary = "预设与标签关联表-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<TsPresetTag>> queryPageList(
            TsPresetTag tsPresetTag,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TsPresetTag> queryWrapper = QueryGenerator.initQueryWrapper(tsPresetTag, req.getParameterMap());
        Page<TsPresetTag> page = new Page<>(pageNo, pageSize);
        IPage<TsPresetTag> pageList = tsPresetTagService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 新增
     */
    @AutoLog(value = "预设与标签关联表-新增")
    @Operation(summary = "预设与标签关联表-新增")
    @PostMapping("/add")
    public Result<?> add(@RequestBody TsPresetTag tsPresetTag) {
        if (oConvertUtils.isEmpty(tsPresetTag.getPresetId()) || oConvertUtils.isEmpty(tsPresetTag.getTagId())) {
            return Result.error("presetId/tagId 不能为空");
        }
        TsTag byId = tsTagService.getById(tsPresetTag.getTagId().trim());
        if (byId == null) {
            return Result.error("标签不存在，请输入正确的标签ID");
        }
        tsPresetTag.setTagId(byId.getId());
        tsPresetTagService.save(tsPresetTag);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "预设与标签关联表-编辑")
    @Operation(summary = "预设与标签关联表-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody TsPresetTag tsPresetTag) {
        if (oConvertUtils.isEmpty(tsPresetTag.getId())) {
            return Result.error("id 不能为空");
        }
        TsPresetTag entity = tsPresetTagService.getById(tsPresetTag.getId());
        if (entity == null) {
            return Result.error("未找到对应记录");
        }
        if (oConvertUtils.isNotEmpty(tsPresetTag.getTagId())) {
            TsTag byId = tsTagService.getById(tsPresetTag.getTagId().trim());
            if (byId == null) {
                return Result.error("标签不存在，请输入正确的标签ID");
            }
            tsPresetTag.setTagId(byId.getId());
        } 
        tsPresetTagService.updateById(tsPresetTag);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id查询
     */
    @AutoLog(value = "预设与标签关联表-通过id查询")
    @Operation(summary = "预设与标签关联表-通过id查询")
    @GetMapping("/queryById")
    public Result<TsPresetTag> queryById(@RequestParam(name = "id") String id) {
        TsPresetTag tsPresetTag = tsPresetTagService.getById(id);
        if (tsPresetTag == null) {
            return Result.error("未找到对应记录");
        }
        return Result.OK(tsPresetTag);
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "预设与标签关联表-通过id删除")
    @Operation(summary = "预设与标签关联表-通过id删除")
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        tsPresetTagService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "预设与标签关联表-批量删除")
    @Operation(summary = "预设与标签关联表-批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        if (oConvertUtils.isEmpty(ids)) {
            return Result.error("ids 不能为空");
        }
        tsPresetTagService.removeByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

}
