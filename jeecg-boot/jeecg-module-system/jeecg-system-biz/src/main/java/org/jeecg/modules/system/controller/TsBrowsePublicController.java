package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileQueryDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryQueryDto;
import org.jeecg.modules.system.mapper.TsRoleImageProfileMapper;
import org.jeecg.modules.system.mapper.TsStoryMapper;
import org.jeecg.modules.system.vo.tsroleimageprofile.TsRoleImageProfilePublicVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryPublicVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Browse public APIs")
@RestController
@Validated
@RequestMapping("/sys")
public class TsBrowsePublicController {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TsStoryMapper tsStoryMapper;
    private final TsRoleImageProfileMapper tsRoleImageProfileMapper;

    public TsBrowsePublicController(TsStoryMapper tsStoryMapper, TsRoleImageProfileMapper tsRoleImageProfileMapper) {
        this.tsStoryMapper = tsStoryMapper;
        this.tsRoleImageProfileMapper = tsRoleImageProfileMapper;
    }

    @Operation(summary = "Public story feed")
    @GetMapping("/ts-stories/public")
    public Result<Page<TsStoryPublicVo>> listPublicStories(TsStoryQueryDto request) {
        int pageNo = normalizePageNo(request == null ? null : request.getPageNo());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        String keyword = trimToNull(request == null ? null : request.getKeyword());
        String storyMode = trimToNull(request == null ? null : request.getStoryMode());

        Page<TsStoryPublicVo> page = new Page<>(pageNo, pageSize);
        Page<TsStoryPublicVo> pageData = tsStoryMapper.selectPublicStoryPage(page, keyword, storyMode);
        return Result.OK(pageData);
    }

    @Operation(summary = "Public role image profile feed")
    @GetMapping("/ts-role-image-profiles/public")
    public Result<Page<TsRoleImageProfilePublicVo>> listPublicRoleImageProfiles(TsRoleImageProfileQueryDto request) {
        int pageNo = normalizePageNo(request == null ? null : request.getPageNo());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        String keyword = trimToNull(request == null ? null : request.getKeyword());
        String styleName = trimToNull(request == null ? null : request.getStyleName());
        String sourceType = trimToNull(request == null ? null : request.getSourceType());

        Page<TsRoleImageProfilePublicVo> page = new Page<>(pageNo, pageSize);
        Page<TsRoleImageProfilePublicVo> pageData = tsRoleImageProfileMapper.selectPublicProfilePage(
                page,
                keyword,
                styleName,
                sourceType
        );
        return Result.OK(pageData);
    }

    private static int normalizePageNo(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_PAGE_NO;
        }
        return value;
    }

    private static int normalizePageSize(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(value, MAX_PAGE_SIZE);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

