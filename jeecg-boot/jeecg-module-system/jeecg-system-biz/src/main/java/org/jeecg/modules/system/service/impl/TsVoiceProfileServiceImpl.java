package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsVoiceProfileValidationAspect;
import org.jeecg.modules.aop.TsVoiceProfileValidationAspect.CheckTsVoiceProfileExists;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfilePreviewDto;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileQueryDto;
import org.jeecg.modules.system.dto.tsvoiceprofile.TsVoiceProfileTagSaveDto;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.entity.TsVoiceProfileTag;
import org.jeecg.modules.system.entity.TsVoiceTag;
import org.jeecg.modules.system.mapper.TsUserVoiceConfigMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileTagMapper;
import org.jeecg.modules.system.mapper.TsVoiceTagMapper;
import org.jeecg.modules.system.po.tsvoiceprofile.TsVoiceProfileQueryPo;
import org.jeecg.modules.system.po.tsvoiceprofile.TsVoiceProfileTagSavePo;
import org.jeecg.modules.system.service.ITsVoiceProfileService;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfilePreviewVo;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVoConverter;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TsVoiceProfileServiceImpl extends ServiceImpl<TsVoiceProfileMapper, TsVoiceProfile>
        implements ITsVoiceProfileService {

    private static final String DEFAULT_PREVIEW_TEXT = "\u8FD9\u662F\u8BD5\u542C\u6587\u672C\uFF0C\u8BF7\u6839\u636E\u97F3\u8272\u53C2\u6570\u64AD\u653E\u3002";
    private static final String MSG_VOICE_NOT_FOUND_OR_DELETED = "\u97F3\u8272\u4E0D\u5B58\u5728\u6216\u5DF2\u5220\u9664";
    private static final String MSG_VOICE_NOT_FOUND_OR_DISABLED = "\u97F3\u8272\u4E0D\u5B58\u5728\u6216\u5DF2\u505C\u7528";

    @Resource
    private TsVoiceProfileTagMapper tsVoiceProfileTagMapper;

    @Resource
    private TsVoiceTagMapper tsVoiceTagMapper;

    @Resource
    private TsUserVoiceConfigMapper tsUserVoiceConfigMapper;

    @Resource
    private IMiniMaxDemoService miniMaxDemoService;

    @Override
    public Result<Page<TsVoiceProfileVo>> pageVoiceProfiles(LoginUser user, TsVoiceProfileQueryDto request) {
        TsVoiceProfileQueryPo queryPo = TsVoiceProfileQueryPo.fromRequest(request);
        Page<TsVoiceProfile> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsVoiceProfile> pageData = baseMapper.selectVoiceProfilePage(page, queryPo);

        List<Long> voiceProfileIds = new ArrayList<>();
        if (pageData.getRecords() != null) {
            for (TsVoiceProfile item : pageData.getRecords()) {
                if (item != null && item.getId() != null) {
                    voiceProfileIds.add(item.getId());
                }
            }
        }

        Map<Long, List<Long>> profileTagIds = new HashMap<>();
        Map<Long, TsVoiceTagVo> tagVoMap = new HashMap<>();
        if (!voiceProfileIds.isEmpty()) {
            List<TsVoiceProfileTag> relList = tsVoiceProfileTagMapper.selectByVoiceProfileIds(voiceProfileIds);
            Set<Long> tagIdSet = new LinkedHashSet<>();
            if (relList != null) {
                for (TsVoiceProfileTag rel : relList) {
                    if (rel == null || rel.getVoiceProfileId() == null || rel.getTagId() == null) {
                        continue;
                    }
                    List<Long> tagIds = profileTagIds.get(rel.getVoiceProfileId());
                    if (tagIds == null) {
                        tagIds = new ArrayList<>();
                        profileTagIds.put(rel.getVoiceProfileId(), tagIds);
                    }
                    tagIds.add(rel.getTagId());
                    tagIdSet.add(rel.getTagId());
                }
            }
            if (!tagIdSet.isEmpty()) {
                List<TsVoiceTag> tags = tsVoiceTagMapper.selectBatchIds(new ArrayList<>(tagIdSet));
                if (tags != null) {
                    for (TsVoiceTag tag : tags) {
                        if (tag != null && tag.getId() != null) {
                            tagVoMap.put(tag.getId(), TsVoiceTagVoConverter.fromEntity(tag));
                        }
                    }
                }
            }
        }
        return Result.OK(TsVoiceProfileVoConverter.fromPage(pageData, profileTagIds, tagVoMap));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsVoiceProfileExists(message = MSG_VOICE_NOT_FOUND_OR_DELETED)
    public Result<?> deleteVoiceProfile(LoginUser user, Long id) {
        TsVoiceProfile profile = TsVoiceProfileValidationAspect.VOICE_PROFILE_CONTEXT.get();
        Long refCount = tsUserVoiceConfigMapper.countBySelectedVoiceProfileId(profile.getId());
        if (refCount != null && refCount > 0L) {
            throw new JeecgBootException("\u97F3\u8272\u5DF2\u88AB\u7528\u6237\u914D\u7F6E\u4F7F\u7528\uFF0C\u65E0\u6CD5\u5220\u9664");
        }
        profile.setStatus(0);
        profile.setUpdatedAt(new Date());
        this.updateById(profile);
        return Result.OK("\u5220\u9664\u6210\u529F");
    }

    @Override
    @CheckTsVoiceProfileExists(message = MSG_VOICE_NOT_FOUND_OR_DELETED)
    public Result<List<TsVoiceTagVo>> getVoiceProfileTags(LoginUser user, Long id) {
        TsVoiceProfile profile = TsVoiceProfileValidationAspect.VOICE_PROFILE_CONTEXT.get();
        List<TsVoiceProfileTag> relList = tsVoiceProfileTagMapper.selectByVoiceProfileIds(
                Collections.singletonList(profile.getId())
        );
        List<Long> tagIds = new ArrayList<>();
        if (relList != null) {
            for (TsVoiceProfileTag rel : relList) {
                if (rel != null && rel.getTagId() != null) {
                    tagIds.add(rel.getTagId());
                }
            }
        }
        if (tagIds.isEmpty()) {
            return Result.OK(new ArrayList<>());
        }

        List<TsVoiceTag> tags = tsVoiceTagMapper.selectBatchIds(tagIds);
        Map<Long, TsVoiceTag> tagMap = new HashMap<>();
        if (tags != null) {
            for (TsVoiceTag tag : tags) {
                if (tag != null && tag.getId() != null) {
                    tagMap.put(tag.getId(), tag);
                }
            }
        }
        List<TsVoiceTagVo> result = new ArrayList<>();
        for (Long tagId : tagIds) {
            TsVoiceTag tag = tagMap.get(tagId);
            if (tag != null) {
                result.add(TsVoiceTagVoConverter.fromEntity(tag));
            }
        }
        return Result.OK(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsVoiceProfileExists(message = MSG_VOICE_NOT_FOUND_OR_DELETED)
    public Result<List<TsVoiceTagVo>> saveVoiceProfileTags(LoginUser user, TsVoiceProfileTagSaveDto request) {
        request.applyDefaults();
        TsVoiceProfileTagSavePo savePo = TsVoiceProfileTagSavePo.fromRequest(request);
        TsVoiceProfile profile = TsVoiceProfileValidationAspect.VOICE_PROFILE_CONTEXT.get();

        List<Long> tagIds = savePo.getTagIds();
        if (tagIds == null) {
            tagIds = new ArrayList<>();
        }
        if (!tagIds.isEmpty()) {
            List<TsVoiceTag> tags = tsVoiceTagMapper.selectBatchIds(tagIds);
            if (tags == null || tags.size() != tagIds.size()) {
                throw new JeecgBootException("\u6807\u7B7E\u4E0D\u5B58\u5728\u6216\u53C2\u6570\u975E\u6CD5");
            }
        }

        tsVoiceProfileTagMapper.deleteByVoiceProfileId(profile.getId());
        if (!tagIds.isEmpty()) {
            tsVoiceProfileTagMapper.insertBatch(profile.getId(), tagIds);
        }
        if (tagIds.isEmpty()) {
            return Result.OK("\u4FDD\u5B58\u6210\u529F", new ArrayList<>());
        }

        List<TsVoiceTag> tags = tsVoiceTagMapper.selectBatchIds(tagIds);
        Map<Long, TsVoiceTag> tagMap = new HashMap<>();
        if (tags != null) {
            for (TsVoiceTag tag : tags) {
                if (tag != null && tag.getId() != null) {
                    tagMap.put(tag.getId(), tag);
                }
            }
        }
        List<TsVoiceTagVo> result = new ArrayList<>();
        for (Long tagId : tagIds) {
            TsVoiceTag tag = tagMap.get(tagId);
            if (tag != null) {
                result.add(TsVoiceTagVoConverter.fromEntity(tag));
            }
        }
        return Result.OK("\u4FDD\u5B58\u6210\u529F", result);
    }

    @Override
    @CheckTsVoiceProfileExists(message = MSG_VOICE_NOT_FOUND_OR_DISABLED)
    public Result<TsVoiceProfilePreviewVo> previewVoice(LoginUser user, TsVoiceProfilePreviewDto request) {
        request.normalize();
        TsVoiceProfile profile = TsVoiceProfileValidationAspect.VOICE_PROFILE_CONTEXT.get();
        if (profile == null) {
            throw new JeecgBootException(MSG_VOICE_NOT_FOUND_OR_DISABLED);
        }
        if (!StringUtils.hasText(profile.getProviderVoiceId())) {
            throw new JeecgBootException("\u5F53\u524D\u97F3\u8272\u672A\u914D\u7F6E providerVoiceId\uFF0C\u65E0\u6CD5\u8FDB\u884C\u8BED\u97F3\u5408\u6210");
        }

        String previewText = StringUtils.hasText(request.getPreviewText()) ? request.getPreviewText() : DEFAULT_PREVIEW_TEXT;
        MiniMaxTtsRequestDto ttsRequest = new MiniMaxTtsRequestDto();
        ttsRequest.setText(previewText);
        ttsRequest.setVoiceId(profile.getProviderVoiceId().trim());
        MiniMaxTtsResponseVo ttsResponse = miniMaxDemoService.tts(ttsRequest);
        String audioUrl = ttsResponse == null ? null : ttsResponse.getAudioUrl();
        if (!StringUtils.hasText(audioUrl)) {
            throw new JeecgBootException("\u8BD5\u542C\u751F\u6210\u6210\u529F\u4F46\u672A\u8FD4\u56DE\u53EF\u64AD\u653E\u5730\u5740\uFF0C\u8BF7\u68C0\u67E5 AIRAG_MINIMAX_UPLOAD_GENERATED_MEDIA \u914D\u7F6E");
        }

        TsVoiceProfilePreviewVo vo = new TsVoiceProfilePreviewVo();
        vo.setVoiceProfileId(profile.getId());
        vo.setVoiceName(profile.getName());
        vo.setProviderVoiceId(profile.getProviderVoiceId());
        vo.setPreviewText(previewText);
        vo.setPreviewAudioUrl(audioUrl);
        return Result.OK(vo);
    }
}
