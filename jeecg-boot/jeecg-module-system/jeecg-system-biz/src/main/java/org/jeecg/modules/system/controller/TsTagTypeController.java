package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.entity.TsTagType;
import org.jeecg.modules.system.service.ITsTagTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Description: 角色与故事固定标签类型
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Tag(name = "角色与故事固定标签类型")
@RestController
@RequestMapping("/sys/tsTagType")
public class TsTagTypeController {

    @Autowired
    private ITsTagTypeService tsTagTypeService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "生成标签类型字典表-分页列表查询")
    @Operation(summary = "生成标签类型字典表-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<TsTagType>> queryPageList(
            TsTagType tsTagType,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TsTagType> queryWrapper = QueryGenerator.initQueryWrapper(tsTagType, req.getParameterMap());
        Page<TsTagType> page = new Page<>(pageNo, pageSize);
        IPage<TsTagType> pageList = tsTagTypeService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 新增
     */
    @AutoLog(value = "生成标签类型字典表-新增")
    @Operation(summary = "生成标签类型字典表-新增")
    @PostMapping("/add")
    public Result<?> add(@RequestBody TsTagType tsTagType) {
        if (oConvertUtils.isEmpty(tsTagType.getId()) || oConvertUtils.isEmpty(tsTagType.getName())) {
            return Result.error("id/name 不能为空");
        }
        if (!"role".equals(tsTagType.getScope()) && !"story".equals(tsTagType.getScope())) {
            return Result.error("scope 仅支持 role 或 story");
        }
        if (tsTagType.getEnabled() == null) {
            tsTagType.setEnabled(1);
        }
        if (tsTagType.getVersion() == null) {
            tsTagType.setVersion(1);
        }
        if (tsTagType.getSortOrder() == null) {
            tsTagType.setSortOrder(0);
        }
        tsTagTypeService.save(tsTagType);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "生成标签类型字典表-编辑")
    @Operation(summary = "生成标签类型字典表-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody TsTagType tsTagType) {
        if (oConvertUtils.isEmpty(tsTagType.getId())) {
            return Result.error("id 不能为空");
        }
        TsTagType entity = tsTagTypeService.getById(tsTagType.getId());
        if (entity == null) {
            return Result.error("未找到对应记录");
        }
        tsTagTypeService.updateById(tsTagType);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id查询
     */
    @AutoLog(value = "生成标签类型字典表-通过id查询")
    @Operation(summary = "生成标签类型字典表-通过id查询")
    @GetMapping("/queryById")
    public Result<TsTagType> queryById(@RequestParam(name = "id") String id) {
        TsTagType tsTagType = tsTagTypeService.getById(id);
        if (tsTagType == null) {
            return Result.error("未找到对应记录");
        }
        return Result.OK(tsTagType);
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "生成标签类型字典表-通过id删除")
    @Operation(summary = "生成标签类型字典表-通过id删除")
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        tsTagTypeService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "生成标签类型字典表-批量删除")
    @Operation(summary = "生成标签类型字典表-批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        if (oConvertUtils.isEmpty(ids)) {
            return Result.error("ids 不能为空");
        }
        tsTagTypeService.removeByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }
}
