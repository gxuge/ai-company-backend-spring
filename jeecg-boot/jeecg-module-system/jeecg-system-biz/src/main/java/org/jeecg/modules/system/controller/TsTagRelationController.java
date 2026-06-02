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
import org.jeecg.modules.system.entity.TsTagRelation;
import org.jeecg.modules.system.service.ITsTagRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Description: 标签关系规则表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Slf4j
@Tag(name = "标签关系规则表")
@RestController
@RequestMapping("/sys/tsTagRelation")
public class TsTagRelationController {

    @Autowired
    private ITsTagRelationService tagRelationService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "标签关系规则表-分页列表查询")
    @Operation(summary = "标签关系规则表-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<TsTagRelation>> queryPageList(
            TsTagRelation tagRelation,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TsTagRelation> queryWrapper = QueryGenerator.initQueryWrapper(tagRelation, req.getParameterMap());
        Page<TsTagRelation> page = new Page<>(pageNo, pageSize);
        IPage<TsTagRelation> pageList = tagRelationService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 新增
     */
    @AutoLog(value = "标签关系规则表-新增")
    @Operation(summary = "标签关系规则表-新增")
    @PostMapping("/add")
    public Result<?> add(@RequestBody TsTagRelation tagRelation) {
        if (oConvertUtils.isEmpty(tagRelation.getSourceTagId())
                || oConvertUtils.isEmpty(tagRelation.getTargetTagId())
                || oConvertUtils.isEmpty(tagRelation.getRelationType())) {
            return Result.error("sourceTagId/targetTagId/relationType 不能为空");
        }
        tagRelationService.save(tagRelation);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "标签关系规则表-编辑")
    @Operation(summary = "标签关系规则表-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody TsTagRelation tagRelation) {
        if (oConvertUtils.isEmpty(tagRelation.getId())) {
            return Result.error("id 不能为空");
        }
        TsTagRelation entity = tagRelationService.getById(tagRelation.getId());
        if (entity == null) {
            return Result.error("未找到对应记录");
        }
        if (oConvertUtils.isEmpty(tagRelation.getSourceTagId())
                || oConvertUtils.isEmpty(tagRelation.getTargetTagId())
                || oConvertUtils.isEmpty(tagRelation.getRelationType())) {
            return Result.error("sourceTagId/targetTagId/relationType 不能为空");
        }
        tagRelationService.updateById(tagRelation);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id查询
     */
    @AutoLog(value = "标签关系规则表-通过id查询")
    @Operation(summary = "标签关系规则表-通过id查询")
    @GetMapping("/queryById")
    public Result<TsTagRelation> queryById(@RequestParam(name = "id") String id) {
        TsTagRelation entity = tagRelationService.getById(id);
        if (entity == null) {
            return Result.error("未找到对应记录");
        }
        return Result.OK(entity);
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "标签关系规则表-通过id删除")
    @Operation(summary = "标签关系规则表-通过id删除")
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        tagRelationService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "标签关系规则表-批量删除")
    @Operation(summary = "标签关系规则表-批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        if (oConvertUtils.isEmpty(ids)) {
            return Result.error("ids 不能为空");
        }
        tagRelationService.removeByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }
}
