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
import org.jeecg.modules.system.entity.TsPresetTag;
import org.jeecg.modules.system.entity.TsTagType;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.service.ITsPresetService;
import org.jeecg.modules.system.service.ITsPresetTagService;
import org.jeecg.modules.system.service.ITsTagTypeService;
import org.jeecg.modules.system.service.ITsTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Description: 生成素材标签主表
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Slf4j
@Tag(name = "生成素材标签主表")
@RestController
@RequestMapping("/sys/tsTag")
public class TsTagController {

    @Autowired
    private ITsTagService tsTagService;
    @Autowired
    private ITsTagTypeService tsTagTypeService;
    @Autowired
    private ITsPresetService tsPresetService;
    @Autowired
    private ITsPresetTagService tsPresetTagService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "生成素材标签主表-分页列表查询")
    @Operation(summary = "生成素材标签主表-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<TsTag>> queryPageList(
            TsTag tsTag,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TsTag> queryWrapper = QueryGenerator.initQueryWrapper(tsTag, req.getParameterMap());
        String presetId = req.getParameter("presetId");
        if (oConvertUtils.isNotEmpty(presetId)) {
            QueryWrapper<TsPresetTag> relationWrapper = new QueryWrapper<>();
            relationWrapper.eq("preset_id", presetId);
            List<TsPresetTag> relations = tsPresetTagService.list(relationWrapper);
            if (relations == null || relations.isEmpty()) {
                queryWrapper.eq("id", "__NO_DATA__");
            } else {
                List<String> tagIds = relations.stream().map(TsPresetTag::getTagId).toList();
                queryWrapper.in("id", tagIds);
            }
        }
        Page<TsTag> page = new Page<>(pageNo, pageSize);
        IPage<TsTag> pageList = tsTagService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 新增
     */
    @AutoLog(value = "生成素材标签主表-新增")
    @Operation(summary = "生成素材标签主表-新增")
    @PostMapping("/add")
    @Transactional
    public Result<?> add(@RequestBody AddTagRequest request) {
        String requestTagName = resolveRequestTagName(request);
        if (oConvertUtils.isNotEmpty(request.getPresetId())) {
            request.setName(requestTagName);
            return quickAddForPreset(request);
        }
        TsTag tsTag = new TsTag();
        tsTag.setScope(request.getScope());
        tsTag.setTypeId(request.getTypeId());
        tsTag.setName(requestTagName);
        tsTag.setDescription(request.getDescription());
        tsTag.setPromptText(request.getPromptText());
        tsTag.setWeight(request.getWeight());
        tsTag.setEnabled(request.getEnabled());
        tsTag.setSortOrder(request.getSortOrder());
        if (oConvertUtils.isEmpty(tsTag.getScope()) || oConvertUtils.isEmpty(tsTag.getTypeId()) || oConvertUtils.isEmpty(tsTag.getName())) {
            return Result.error("scope/typeId/name 不能为空");
        }
        if (!isValidTagType(tsTag.getTypeId())) {
            return Result.error("typeId 不存在，请选择正确的标签类型");
        }
        tsTagService.save(tsTag);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "生成素材标签主表-编辑")
    @Operation(summary = "生成素材标签主表-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody TsTag tsTag) {
        if (oConvertUtils.isEmpty(tsTag.getId())) {
            return Result.error("id 不能为空");
        }
        TsTag entity = tsTagService.getById(tsTag.getId());
        if (entity == null) {
            return Result.error("未找到对应记录");
        }
        if (oConvertUtils.isNotEmpty(tsTag.getTypeId()) && !isValidTagType(tsTag.getTypeId())) {
            return Result.error("typeId 不存在，请选择正确的标签类型");
        }
        tsTagService.updateById(tsTag);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id查询
     */
    @AutoLog(value = "生成素材标签主表-通过id查询")
    @Operation(summary = "生成素材标签主表-通过id查询")
    @GetMapping("/queryById")
    public Result<TsTag> queryById(@RequestParam(name = "id") String id) {
        TsTag tsTag = tsTagService.getById(id);
        if (tsTag == null) {
            return Result.error("未找到对应记录");
        }
        return Result.OK(tsTag);
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "生成素材标签主表-通过id删除")
    @Operation(summary = "生成素材标签主表-通过id删除")
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        tsTagService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "生成素材标签主表-批量删除")
    @Operation(summary = "生成素材标签主表-批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        if (oConvertUtils.isEmpty(ids)) {
            return Result.error("ids 不能为空");
        }
        tsTagService.removeByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    private boolean isValidTagType(String typeCode) {
        TsTagType tsTagType = tsTagTypeService.getById(typeCode);
        return tsTagType != null && Integer.valueOf(1).equals(tsTagType.getEnabled());
    }

    private Result<?> quickAddForPreset(AddTagRequest request) {
        String requestTagName = resolveRequestTagName(request);
        if (oConvertUtils.isEmpty(request.getPresetId()) || oConvertUtils.isEmpty(requestTagName)) {
            return Result.error("presetId/tagName 不能为空");
        }
        TsPreset preset = tsPresetService.getById(request.getPresetId());
        if (preset == null) {
            return Result.error("预设不存在，请确认 presetId");
        }
        TsTag tag = new TsTag();
        tag.setName(requestTagName.trim());
        tag.setScope(resolveScopeByPresetTarget(preset.getTargetType()));
        String requestTypeId = oConvertUtils.isNotEmpty(request.getTypeId())
                ? request.getTypeId().trim()
                : resolveTypeIdByPresetTarget(preset.getTargetType());
        tag.setTypeId(requestTypeId);
        tag.setEnabled(1);
        tag.setWeight(100);
        tag.setSortOrder(0);
        if (!isValidTagType(tag.getTypeId())) {
            return Result.error("typeId 不存在，请选择正确的标签类型");
        }
        tsTagService.save(tag);

        TsPresetTag relation = new TsPresetTag();
        relation.setPresetId(request.getPresetId());
        relation.setTagId(tag.getId());
        relation.setRequired(request.getRequired() == null ? 0 : request.getRequired());
        relation.setWeightOverride(request.getWeightOverride());
        relation.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        tsPresetTagService.save(relation);
        return Result.OK("添加成功！");
    }

    private String resolveScopeByPresetTarget(String targetType) {
        if ("story".equalsIgnoreCase(targetType)) {
            return "story";
        }
        if ("both".equalsIgnoreCase(targetType)) {
            return "shared";
        }
        return "character";
    }

    private String resolveTypeIdByPresetTarget(String targetType) {
        if ("story".equalsIgnoreCase(targetType)) {
            return "title";
        }
        return "identity";
    }

    @lombok.Data
    private static class AddTagRequest {
        private String presetId;
        private String scope;
        private String typeId;
        private String tagName;
        private String name;
        private String description;
        private String promptText;
        private Integer weight;
        private Integer enabled;
        private Integer sortOrder;
        private Integer required;
        private Integer weightOverride;
    }

    private String resolveRequestTagName(AddTagRequest request) {
        if (oConvertUtils.isNotEmpty(request.getTagName())) {
            return request.getTagName();
        }
        return request.getName();
    }
}
