package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuservoiceprofile.TsUserVoiceProfileRenameDto;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfilePreviewDto;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileQueryDto;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileTagSaveDto;
import org.jeecg.modules.system.service.ITsVoiceProfileService;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfilePreviewVo;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "TsVoiceProfile")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsVoiceProfileController {

    @Autowired
    private ITsVoiceProfileService tsVoiceProfileService;

    @Operation(summary = "Public voice profile page query")
    @GetMapping("/ts-voice-profiles")
    public Result<Page<TsVoiceProfileVo>> listVoiceProfiles(TsVoiceProfileQueryDto request) {
        return tsVoiceProfileService.pageVoiceProfiles((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }

    @Operation(summary = "Current user voice library page query")
    @GetMapping("/ts-user-voice-profiles")
    public Result<Page<TsVoiceProfileVo>> listUserVoiceProfiles(TsVoiceProfileQueryDto request) {
        return tsVoiceProfileService.pageUserVoiceProfiles((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }

    @Operation(summary = "Rename a voice in current user library")
    @PutMapping("/ts-user-voice-profiles/{id}")
    public Result<TsVoiceProfileVo> renameUserVoiceProfile(
            @PathVariable("id") Long id,
            @Validated @RequestBody TsUserVoiceProfileRenameDto request) {
        return tsVoiceProfileService.renameUserVoiceProfile((LoginUser) SecurityUtils.getSubject().getPrincipal(), id, request);
    }

    @Operation(summary = "Delete a voice from current user library")
    @DeleteMapping("/ts-user-voice-profiles/{id}")
    public Result<?> removeUserVoiceProfile(@PathVariable("id") Long id) {
        return tsVoiceProfileService.deleteUserVoiceProfile((LoginUser) SecurityUtils.getSubject().getPrincipal(), id);
    }

    @Operation(summary = "Delete public voice profile")
    @DeleteMapping("/ts-voice-profiles")
    public Result<?> removeVoiceProfile(@RequestParam("id") Long id) {
        return tsVoiceProfileService.deleteVoiceProfile((LoginUser) SecurityUtils.getSubject().getPrincipal(), id);
    }

    @Operation(summary = "Query voice profile tags")
    @GetMapping("/ts-voice-profiles/tags")
    public Result<List<TsVoiceTagVo>> getVoiceProfileTags(@RequestParam("id") Long id) {
        return tsVoiceProfileService.getVoiceProfileTags((LoginUser) SecurityUtils.getSubject().getPrincipal(), id);
    }

    @Operation(summary = "Save voice profile tags")
    @PutMapping("/ts-voice-profiles/tags")
    public Result<List<TsVoiceTagVo>> saveVoiceProfileTags(@Validated @RequestBody TsVoiceProfileTagSaveDto request) {
        return tsVoiceProfileService.saveVoiceProfileTags((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }

    @Operation(summary = "Generate preview audio by selected voice profile")
    @PostMapping("/ts-voice-profiles/preview")
    public Result<TsVoiceProfilePreviewVo> previewVoice(@Validated @RequestBody TsVoiceProfilePreviewDto request) {
        return tsVoiceProfileService.previewVoice((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }
}

