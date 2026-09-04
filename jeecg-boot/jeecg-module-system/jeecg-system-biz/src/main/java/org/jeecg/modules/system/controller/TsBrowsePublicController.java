package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileQueryDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicBrowseQueryDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicBrowseQueryDto;
import org.jeecg.modules.system.mapper.TsRoleImageProfileMapper;
import org.jeecg.modules.system.mapper.TsRolePublicMapper;
import org.jeecg.modules.system.mapper.TsStoryPublicMapper;
import org.jeecg.modules.system.service.ITsContentTagService;
import org.jeecg.modules.system.vo.tscontenttag.TsContentTagDisplayVo;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceResolver;
import org.jeecg.modules.system.vo.tsroleimageprofile.TsRoleImageProfilePublicVo;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicBrowseVo;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicBrowseVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Browse public APIs")
@RestController
@Validated
@RequestMapping("/sys")
public class TsBrowsePublicController {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TsStoryPublicMapper tsStoryPublicMapper;
    private final TsRolePublicMapper tsRolePublicMapper;
    private final TsRoleImageProfileMapper tsRoleImageProfileMapper;
    private final ITsContentTagService tsContentTagService;

    public TsBrowsePublicController(TsStoryPublicMapper tsStoryPublicMapper,
                                    TsRolePublicMapper tsRolePublicMapper,
                                    TsRoleImageProfileMapper tsRoleImageProfileMapper,
                                    ITsContentTagService tsContentTagService) {
        this.tsStoryPublicMapper = tsStoryPublicMapper;
        this.tsRolePublicMapper = tsRolePublicMapper;
        this.tsRoleImageProfileMapper = tsRoleImageProfileMapper;
        this.tsContentTagService = tsContentTagService;
    }

    @Operation(summary = "Public story feed")
    @GetMapping("/ts-stories/public")
    public Result<Page<TsStoryPublicBrowseVo>> listPublicStories(TsStoryPublicBrowseQueryDto request) {
        int pageNo = normalizePageNo(request == null ? null : request.getPageNo());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        Page<TsStoryPublicBrowseVo> page = new Page<>(pageNo, pageSize);
        Page<TsStoryPublicBrowseVo> pageData = tsStoryPublicMapper.selectPublicBrowsePage(page, request);
        enrichStoryBrowseRecords(pageData.getRecords());
        return Result.OK(pageData);
    }

    @Operation(summary = "Public story detail")
    @GetMapping("/ts-stories/public/detail")
    public Result<TsStoryPublicBrowseVo> getPublicStory(@RequestParam(value = "id", required = false) Long id,
                                                        @RequestParam(value = "publicId", required = false) Long publicId,
                                                        @RequestParam(value = "channelCode", required = false) String channelCode) {
        if (id == null && publicId == null && !org.springframework.util.StringUtils.hasText(channelCode)) {
            return Result.error("id、publicId、channelCode至少传一个");
        }
        TsStoryPublicBrowseVo detail = tsStoryPublicMapper.selectPublicBrowseDetail(id, publicId, trimToNull(channelCode));
        if (detail == null) {
            return Result.error("公开故事不存在");
        }
        enrichStoryBrowse(detail);
        return Result.OK(detail);
    }

    @Operation(summary = "Public role feed")
    @GetMapping("/ts-roles/public")
    public Result<Page<TsRolePublicBrowseVo>> listPublicRoles(TsRolePublicBrowseQueryDto request) {
        int pageNo = normalizePageNo(request == null ? null : request.getPageNo());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        Page<TsRolePublicBrowseVo> page = new Page<>(pageNo, pageSize);
        Page<TsRolePublicBrowseVo> pageData = tsRolePublicMapper.selectPublicBrowsePage(page, request);
        enrichRoleBrowseRecords(pageData.getRecords());
        return Result.OK(pageData);
    }

    @Operation(summary = "Public role detail")
    @GetMapping("/ts-roles/public/detail")
    public Result<TsRolePublicBrowseVo> getPublicRole(@RequestParam(value = "id", required = false) Long id,
                                                      @RequestParam(value = "publicId", required = false) Long publicId,
                                                      @RequestParam(value = "channelCode", required = false) String channelCode) {
        if (id == null && publicId == null && !org.springframework.util.StringUtils.hasText(channelCode)) {
            return Result.error("id、publicId、channelCode至少传一个");
        }
        TsRolePublicBrowseVo detail = tsRolePublicMapper.selectPublicBrowseDetail(id, publicId, trimToNull(channelCode));
        if (detail == null) {
            return Result.error("公开角色不存在");
        }
        enrichRoleBrowse(detail);
        return Result.OK(detail);
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
        enrichRoleImageProfileRecords(pageData.getRecords());
        return Result.OK(pageData);
    }

    private void enrichStoryBrowseRecords(List<TsStoryPublicBrowseVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (TsStoryPublicBrowseVo item : records) {
            enrichStoryBrowseImage(item);
        }
        Map<Long, Integer> versions = new LinkedHashMap<>();
        for (TsStoryPublicBrowseVo item : records) {
            if (item != null && item.getId() != null && item.getContentVersion() != null) {
                versions.put(item.getId(), item.getContentVersion());
            }
        }
        Map<Long, List<TsContentTagDisplayVo>> tags =
                tsContentTagService.findCurrentDisplayTags("story", versions);
        for (TsStoryPublicBrowseVo item : records) {
            if (item != null) {
                item.setTags(tags.getOrDefault(item.getId(), List.of()));
            }
        }
    }

    private void enrichStoryBrowse(TsStoryPublicBrowseVo item) {
        if (item == null) {
            return;
        }
        enrichStoryBrowseImage(item);
        if (item.getId() == null || item.getContentVersion() == null) {
            item.setTags(List.of());
            return;
        }
        item.setTags(tsContentTagService.findCurrentDisplayTags(
                "story", Map.of(item.getId(), item.getContentVersion()))
                .getOrDefault(item.getId(), List.of()));
    }

    private void enrichStoryBrowseImage(TsStoryPublicBrowseVo item) {
        if (item == null) {
            return;
        }
        item.setImageResources(TsImageResourceResolver.buildStoryPublicBrowseImageResources(
                item.getId(),
                item.getSceneImageUrl(),
                item.getCoverUrl(),
                item.getAuthorAvatar()));
    }

    private void enrichRoleBrowseRecords(List<TsRolePublicBrowseVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (TsRolePublicBrowseVo item : records) {
            enrichRoleBrowseImage(item);
        }
        Map<Long, Integer> versions = new LinkedHashMap<>();
        for (TsRolePublicBrowseVo item : records) {
            if (item != null && item.getId() != null && item.getContentVersion() != null) {
                versions.put(item.getId(), item.getContentVersion());
            }
        }
        Map<Long, List<TsContentTagDisplayVo>> tags =
                tsContentTagService.findCurrentDisplayTags("role", versions);
        for (TsRolePublicBrowseVo item : records) {
            if (item != null) {
                item.setTags(tags.getOrDefault(item.getId(), List.of()));
            }
        }
    }

    private void enrichRoleBrowse(TsRolePublicBrowseVo item) {
        if (item == null) {
            return;
        }
        enrichRoleBrowseImage(item);
        if (item.getId() == null || item.getContentVersion() == null) {
            item.setTags(List.of());
            return;
        }
        item.setTags(tsContentTagService.findCurrentDisplayTags(
                "role", Map.of(item.getId(), item.getContentVersion()))
                .getOrDefault(item.getId(), List.of()));
    }

    private void enrichRoleBrowseImage(TsRolePublicBrowseVo item) {
        if (item == null) {
            return;
        }
        item.setImageResources(TsImageResourceResolver.buildRolePublicBrowseImageResources(
                item.getId(),
                item.getAvatarUrl(),
                item.getCoverUrl(),
                item.getAuthorAvatar()));
    }

    private void enrichRoleImageProfileRecords(List<TsRoleImageProfilePublicVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (TsRoleImageProfilePublicVo item : records) {
            if (item == null) {
                continue;
            }
            item.setImageResources(TsImageResourceResolver.buildRoleImageProfilePublicResources(
                    item.getSelectedImageUrl(),
                    item.getAuthorAvatar()));
        }
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
