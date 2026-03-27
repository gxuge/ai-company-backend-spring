package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetQueryDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetSaveDto;
import org.jeecg.modules.system.service.ITsUserImageAssetService;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVo;
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
@Tag(name = "TsUserImageAsset 用户图片素材")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsUserImageAssetController {

    @Autowired
    private ITsUserImageAssetService tsUserImageAssetService;
    @Operation(summary = "用户图片素材分页查询")
    @GetMapping("/ts-user-image-assets")
    public Result<Page<TsUserImageAssetVo>> listAssets(TsUserImageAssetQueryDto request) {
        return tsUserImageAssetService.pageAssets(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "用户图片素材详情")
    @GetMapping("/ts-user-image-assets/detail")
    public Result<TsUserImageAssetVo> getAsset(@RequestParam("id") Long id) {
        return tsUserImageAssetService.getAsset(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增用户图片素材")
    @PostMapping("/ts-user-image-assets")
    public Result<TsUserImageAssetVo> createAsset(
            @Validated(TsUserImageAssetSaveDto.Create.class) @RequestBody TsUserImageAssetSaveDto request) {
        return tsUserImageAssetService.addAsset(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑用户图片素材")
    @PutMapping("/ts-user-image-assets")
    public Result<TsUserImageAssetVo> updateAsset(
            @Validated(TsUserImageAssetSaveDto.Update.class) @RequestBody TsUserImageAssetSaveDto request) {
        return tsUserImageAssetService.editAsset(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除用户图片素材")
    @DeleteMapping("/ts-user-image-assets")
    public Result<?> removeAsset(@RequestParam("id") Long id) {
        return tsUserImageAssetService.deleteAsset(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}
