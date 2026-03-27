package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstorychapter.TsStoryChapterQueryDto;
import org.jeecg.modules.system.dto.tsstorychapter.TsStoryChapterSaveDto;
import org.jeecg.modules.system.service.ITsStoryChapterService;
import org.jeecg.modules.system.vo.tsstorychapter.TsStoryChapterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Slf4j
@Tag(name = "TsStory 章节与剧情")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsStoryChapterController {

    @Autowired
    private ITsStoryChapterService tsStoryChapterService;
    @Operation(summary = "章节分页查询")
    @GetMapping("/ts-story-chapters")
    public Result<Page<TsStoryChapterVo>> listChapters(@Validated TsStoryChapterQueryDto request) {
        return tsStoryChapterService.pageChapters(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "章节详情")
    @GetMapping("/ts-story-chapters/detail")
    public Result<TsStoryChapterVo> getChapter(@RequestParam("id") Long id) {
        return tsStoryChapterService.getChapter(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增章节")
    @PostMapping("/ts-story-chapters")
    public Result<TsStoryChapterVo> createChapter(@Validated(TsStoryChapterSaveDto.Create.class) @RequestBody TsStoryChapterSaveDto request) {
        return tsStoryChapterService.addChapter(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑章节")
    @PutMapping("/ts-story-chapters")
    public Result<TsStoryChapterVo> updateChapter(@Validated(TsStoryChapterSaveDto.Update.class) @RequestBody TsStoryChapterSaveDto request) {
        return tsStoryChapterService.editChapter(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除章节")
    @DeleteMapping("/ts-story-chapters")
    public Result<?> removeChapter(@RequestParam("id") Long id) {
        return tsStoryChapterService.deleteChapter(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}
