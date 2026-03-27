package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsvoicetag.TsVoiceTagQueryDto;
import org.jeecg.modules.system.dto.tsvoicetag.TsVoiceTagSaveDto;
import org.jeecg.modules.system.service.ITsVoiceTagService;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@Tag(name = "TsVoiceTag 音色标签")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsVoiceTagController {

    @Autowired
    private ITsVoiceTagService tsVoiceTagService;
    @Operation(summary = "音色标签分页查询")
    @GetMapping("/ts-voice-tags")
    public Result<Page<TsVoiceTagVo>> listVoiceTags(TsVoiceTagQueryDto request) {
        return tsVoiceTagService.pageVoiceTags((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }
    @Operation(summary = "新增音色标签")
    @PostMapping("/ts-voice-tags")
    public Result<TsVoiceTagVo> createVoiceTag(@Validated @RequestBody TsVoiceTagSaveDto request) {
        return tsVoiceTagService.addVoiceTag((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }
    @Operation(summary = "删除音色标签")
    @DeleteMapping("/ts-voice-tags")
    public Result<?> removeVoiceTag(@RequestParam("id") Long id) {
        return tsVoiceTagService.deleteVoiceTag((LoginUser) SecurityUtils.getSubject().getPrincipal(), id);
    }
}
