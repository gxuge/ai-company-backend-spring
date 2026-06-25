package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspublicchannel.TsPublicChannelQueryDto;
import org.jeecg.modules.system.dto.tspublicchannel.TsPublicChannelSaveDto;
import org.jeecg.modules.system.service.ITsPublicChannelService;
import org.jeecg.modules.system.vo.tspublicchannel.TsPublicChannelOptionVo;
import org.jeecg.modules.system.vo.tspublicchannel.TsPublicChannelVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公开渠道管理接口。
 */
@Slf4j
@Tag(name = "TsPublicChannel 公开渠道")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsPublicChannelController {

    private final ITsPublicChannelService tsPublicChannelService;

    public TsPublicChannelController(ITsPublicChannelService tsPublicChannelService) {
        this.tsPublicChannelService = tsPublicChannelService;
    }

    @Operation(summary = "公开渠道分页查询")
    @GetMapping("/ts-public-channels")
    public Result<Page<TsPublicChannelVo>> listChannels(TsPublicChannelQueryDto request) {
        return tsPublicChannelService.pageChannels(currentUser(), request);
    }

    @Operation(summary = "公开渠道详情")
    @GetMapping("/ts-public-channels/detail")
    public Result<TsPublicChannelVo> getChannel(@RequestParam("id") Long id) {
        return tsPublicChannelService.getChannel(currentUser(), id);
    }

    @Operation(summary = "新增公开渠道")
    @PostMapping("/ts-public-channels")
    public Result<TsPublicChannelVo> createChannel(
            @Validated(TsPublicChannelSaveDto.Create.class) @RequestBody TsPublicChannelSaveDto request) {
        return tsPublicChannelService.addChannel(currentUser(), request);
    }

    @Operation(summary = "编辑公开渠道")
    @PutMapping("/ts-public-channels")
    public Result<TsPublicChannelVo> updateChannel(
            @Validated(TsPublicChannelSaveDto.Update.class) @RequestBody TsPublicChannelSaveDto request) {
        return tsPublicChannelService.editChannel(currentUser(), request.getId(), request);
    }

    @Operation(summary = "删除公开渠道")
    @DeleteMapping("/ts-public-channels")
    public Result<?> deleteChannel(@RequestParam("id") Long id) {
        return tsPublicChannelService.deleteChannel(currentUser(), id);
    }

    @Operation(summary = "公开渠道下拉选项")
    @GetMapping("/ts-public-channels/options")
    public Result<List<TsPublicChannelOptionVo>> listChannelOptions(@RequestParam("targetType") String targetType) {
        return tsPublicChannelService.listChannelOptions(currentUser(), targetType);
    }

    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
