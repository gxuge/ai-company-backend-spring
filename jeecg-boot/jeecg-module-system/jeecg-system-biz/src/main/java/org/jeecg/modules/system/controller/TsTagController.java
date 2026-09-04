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
import org.jeecg.modules.system.entity.TsTagType;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.service.ITsTagTypeService;
import org.jeecg.modules.system.service.ITsTagService;
import org.jeecg.modules.system.service.ITsContentTagTaskService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.Executor;

/**
 * @Description: 角色与故事固定标签词典
 * @Author: jeecg-boot
 * @Date: 2026-05-28
 * @Version: V1.0
 */
@Slf4j
@Tag(name = "角色与故事固定标签词典")
@RestController
@RequestMapping("/sys/tsTag")
public class TsTagController {

    @Autowired
    private ITsTagService tsTagService;
    @Autowired
    private ITsTagTypeService tsTagTypeService;
    @Autowired
    private ITsContentTagTaskService tsContentTagTaskService;
    @Autowired
    @Qualifier("tsContentTagExecutor")
    private Executor tsContentTagExecutor;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "固定内容标签-分页列表查询")
    @Operation(summary = "固定内容标签-分页列表查询")
    @GetMapping("/list")
    public Result<IPage<TsTag>> queryPageList(
            TsTag tsTag,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TsTag> queryWrapper = QueryGenerator.initQueryWrapper(tsTag, req.getParameterMap());
        Page<TsTag> page = new Page<>(pageNo, pageSize);
        IPage<TsTag> pageList = tsTagService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 新增
     */
    @AutoLog(value = "固定内容标签-新增")
    @Operation(summary = "固定内容标签-新增")
    @PostMapping("/add")
    @Transactional
    public Result<?> add(@RequestBody AddTagRequest request) {
        String requestTagName = resolveRequestTagName(request);
        TsTag tsTag = new TsTag();
        tsTag.setScope(request.getScope());
        tsTag.setTypeId(request.getTypeId());
        tsTag.setName(requestTagName);
        tsTag.setDescription(request.getDescription());
        tsTag.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        tsTag.setVersion(request.getVersion() == null ? 1 : request.getVersion());
        tsTag.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        if (oConvertUtils.isEmpty(tsTag.getScope()) || oConvertUtils.isEmpty(tsTag.getTypeId()) || oConvertUtils.isEmpty(tsTag.getName())) {
            return Result.error("scope/typeId/name 不能为空");
        }
        if (!isValidTagType(tsTag.getTypeId(), tsTag.getScope())) {
            return Result.error("typeId 不存在、未启用或与 scope 不匹配");
        }
        tsTagService.save(tsTag);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "固定内容标签-编辑")
    @Operation(summary = "固定内容标签-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody TsTag tsTag) {
        if (oConvertUtils.isEmpty(tsTag.getId())) {
            return Result.error("id 不能为空");
        }
        TsTag entity = tsTagService.getById(tsTag.getId());
        if (entity == null) {
            return Result.error("未找到对应记录");
        }
        String effectiveTypeId = oConvertUtils.isNotEmpty(tsTag.getTypeId())
                ? tsTag.getTypeId() : entity.getTypeId();
        String effectiveScope = oConvertUtils.isNotEmpty(tsTag.getScope())
                ? tsTag.getScope() : entity.getScope();
        if (!isValidTagType(effectiveTypeId, effectiveScope)) {
            return Result.error("typeId 不存在、未启用或与 scope 不匹配");
        }
        tsTagService.updateById(tsTag);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id查询
     */
    @AutoLog(value = "固定内容标签-通过id查询")
    @Operation(summary = "固定内容标签-通过id查询")
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
    @AutoLog(value = "固定内容标签-通过id删除")
    @Operation(summary = "固定内容标签-通过id删除")
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam(name = "id") String id) {
        tsTagService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "固定内容标签-批量删除")
    @Operation(summary = "固定内容标签-批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids") String ids) {
        if (oConvertUtils.isEmpty(ids)) {
            return Result.error("ids 不能为空");
        }
        tsTagService.removeByIds(java.util.Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    /**
     * 重试失败的内容标签任务。
     */
    @PostMapping("/tasks/retry")
    public Result<Long> retryTask(@RequestParam(name = "taskId") Long taskId) {
        Long acceptedTaskId = tsContentTagTaskService.retry(taskId);
        tsContentTagExecutor.execute(() -> tsContentTagTaskService.execute(acceptedTaskId));
        return Result.OK("已重新提交", acceptedTaskId);
    }

    private boolean isValidTagType(String typeCode, String scope) {
        TsTagType tsTagType = tsTagTypeService.getById(typeCode);
        return tsTagType != null
                && Integer.valueOf(1).equals(tsTagType.getEnabled())
                && tsTagType.getScope().equals(scope);
    }

    @lombok.Data
    private static class AddTagRequest {
        private String scope;
        private String typeId;
        private String tagName;
        private String name;
        private String description;
        private Integer enabled;
        private Integer version;
        private Integer sortOrder;
    }

    private String resolveRequestTagName(AddTagRequest request) {
        if (oConvertUtils.isNotEmpty(request.getTagName())) {
            return request.getTagName();
        }
        return request.getName();
    }
}
