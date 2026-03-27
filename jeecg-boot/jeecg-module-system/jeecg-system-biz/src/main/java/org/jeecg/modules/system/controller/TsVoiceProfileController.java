package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileQueryDto;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileTagSaveDto;
import org.jeecg.modules.system.service.ITsVoiceProfileService;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Slf4j
@Tag(name = "TsVoiceProfile 公共音色")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsVoiceProfileController {

    @Autowired
    private ITsVoiceProfileService tsVoiceProfileService;
    @Operation(summary = "公共音色分页查询")
    @GetMapping("/ts-voice-profiles")
    public Result<Page<TsVoiceProfileVo>> listVoiceProfiles(TsVoiceProfileQueryDto request) {
        return tsVoiceProfileService.pageVoiceProfiles((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }
    @Operation(summary = "删除公共音色")
    @DeleteMapping("/ts-voice-profiles")
    public Result<?> removeVoiceProfile(@RequestParam("id") Long id) {
        return tsVoiceProfileService.deleteVoiceProfile((LoginUser) SecurityUtils.getSubject().getPrincipal(), id);
    }
    @Operation(summary = "查询音色标签")
    @GetMapping("/ts-voice-profiles/tags")
    public Result<List<TsVoiceTagVo>> getVoiceProfileTags(@RequestParam("id") Long id) {
        return tsVoiceProfileService.getVoiceProfileTags((LoginUser) SecurityUtils.getSubject().getPrincipal(), id);
    }
    @Operation(summary = "保存音色标签关系")
    @PutMapping("/ts-voice-profiles/tags")
    public Result<List<TsVoiceTagVo>> saveVoiceProfileTags(@Validated @RequestBody TsVoiceProfileTagSaveDto request) {
        return tsVoiceProfileService.saveVoiceProfileTags((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }
}
