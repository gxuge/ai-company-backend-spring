package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.service.ITsRoleService;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Image 通用生图")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsAiImageController {

    @Autowired
    private ITsRoleService tsRoleService;

    @Operation(summary = "生成临时AI图片")
    @PostMapping("/ai-images/generate")
    public Result<TsRoleOneClickImageGenerateVo> generateImage(
            @RequestBody TsRoleOneClickImageGenerateDto request) {
        return tsRoleService.generateRoleImage(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
}
