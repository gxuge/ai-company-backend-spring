package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuservoiceconfig.TsUserVoiceConfigSaveDto;
import org.jeecg.modules.system.service.ITsUserVoiceConfigService;
import org.jeecg.modules.system.vo.tsuservoiceconfig.TsUserVoiceConfigVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@Tag(name = "TsUserVoiceConfig 用户音色配置")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsUserVoiceConfigController {

    @Autowired
    private ITsUserVoiceConfigService tsUserVoiceConfigService;
    @Operation(summary = "查询当前用户音色配置")
    @GetMapping("/ts-user-voice-config/current")
    public Result<TsUserVoiceConfigVo> getCurrentConfig() {
        return tsUserVoiceConfigService.getCurrentConfig((LoginUser) SecurityUtils.getSubject().getPrincipal());
    }
    @Operation(summary = "保存当前用户音色配置")
    @PutMapping("/ts-user-voice-config/current")
    public Result<TsUserVoiceConfigVo> saveCurrentConfig(@Validated @RequestBody TsUserVoiceConfigSaveDto request) {
        return tsUserVoiceConfigService.saveCurrentConfig((LoginUser) SecurityUtils.getSubject().getPrincipal(), request);
    }
}
