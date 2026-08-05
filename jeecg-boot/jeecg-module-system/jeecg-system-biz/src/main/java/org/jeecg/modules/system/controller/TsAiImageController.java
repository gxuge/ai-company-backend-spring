package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsimage.TsImageDownloadDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.service.ITsImageService;
import org.jeecg.modules.system.service.ITsRoleService;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "AI Image 通用生图")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsAiImageController {

    @Autowired
    private ITsRoleService tsRoleService;

    @Autowired
    private ITsImageService tsImageService;

    @Operation(summary = "生成临时AI图片")
    @PostMapping("/ai-images/generate")
    public Result<TsRoleOneClickImageGenerateVo> generateImage(
            @RequestBody TsRoleOneClickImageGenerateDto request) {
        return tsRoleService.generateRoleImage(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }

    /**
     * 代理下载临时 AI 图片，不执行图片入库。
     *
     * @param request 下载请求
     * @param response HTTP 响应
     */
    @Operation(summary = "下载临时AI图片")
    @PostMapping("/ts-images/download")
    public void downloadImage(
            @Validated @RequestBody TsImageDownloadDto request,
            HttpServletResponse response) {
        tsImageService.downloadImage(request, response);
    }
}
