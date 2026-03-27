package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordQueryDto;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordSaveDto;
import org.jeecg.modules.system.service.ITsRoleImageGenerateRecordService;
import org.jeecg.modules.system.vo.tsroleimagegeneraterecord.TsRoleImageGenerateRecordVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@Tag(name = "TsRoleImageGenerateRecord 角色图片生成记录")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsRoleImageGenerateRecordController {

    @Autowired
    private ITsRoleImageGenerateRecordService tsRoleImageGenerateRecordService;
    @Operation(summary = "生成记录分页查询")
    @GetMapping("/ts-role-image-generate-records")
    public Result<Page<TsRoleImageGenerateRecordVo>> listRecords(TsRoleImageGenerateRecordQueryDto request) {
        return tsRoleImageGenerateRecordService.pageRecords(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "生成记录详情")
    @GetMapping("/ts-role-image-generate-records/detail")
    public Result<TsRoleImageGenerateRecordVo> getRecord(@RequestParam("id") Long id) {
        return tsRoleImageGenerateRecordService.getRecord(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增生成记录")
    @PostMapping("/ts-role-image-generate-records")
    public Result<TsRoleImageGenerateRecordVo> createRecord(
            @Validated(TsRoleImageGenerateRecordSaveDto.Create.class) @RequestBody TsRoleImageGenerateRecordSaveDto request) {
        return tsRoleImageGenerateRecordService.addRecord(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getRoleId(), request);
    }
    @Operation(summary = "编辑生成记录")
    @PutMapping("/ts-role-image-generate-records")
    public Result<TsRoleImageGenerateRecordVo> updateRecord(
            @Validated(TsRoleImageGenerateRecordSaveDto.Update.class) @RequestBody TsRoleImageGenerateRecordSaveDto request) {
        return tsRoleImageGenerateRecordService.editRecord(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除生成记录")
    @DeleteMapping("/ts-role-image-generate-records")
    public Result<?> removeRecord(@RequestParam("id") Long id) {
        return tsRoleImageGenerateRecordService.deleteRecord(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}
