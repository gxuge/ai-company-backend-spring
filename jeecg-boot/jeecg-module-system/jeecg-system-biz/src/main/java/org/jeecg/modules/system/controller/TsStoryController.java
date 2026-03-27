package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstory.TsStoryQueryDto;
import org.jeecg.modules.system.dto.tsstory.TsStorySaveDto;
import org.jeecg.modules.system.service.ITsStoryService;
import org.jeecg.modules.system.vo.tsstory.TsStoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Slf4j
@Tag(name = "TsStory 故事核心")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsStoryController {

    @Autowired
    private ITsStoryService tsStoryService;
    @Operation(summary = "故事分页查询")
    @GetMapping("/ts-stories")
    public Result<Page<TsStoryVo>> listStories(TsStoryQueryDto request) {
        return tsStoryService.pageStories(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "故事详情")
    @GetMapping("/ts-stories/detail")
    public Result<TsStoryVo> getStory(@RequestParam("id") Long id) {
        return tsStoryService.getStory(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增故事")
    @PostMapping("/ts-stories")
    public Result<TsStoryVo> createStory(@Validated(TsStorySaveDto.Create.class) @RequestBody TsStorySaveDto request) {
        return tsStoryService.addStory(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑故事")
    @PutMapping("/ts-stories")
    public Result<TsStoryVo> updateStory(@Validated(TsStorySaveDto.Update.class) @RequestBody TsStorySaveDto request) {
        return tsStoryService.editStory(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除故事")
    @DeleteMapping("/ts-stories")
    public Result<?> removeStory(@RequestParam("id") Long id) {
        return tsStoryService.deleteStory(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}
