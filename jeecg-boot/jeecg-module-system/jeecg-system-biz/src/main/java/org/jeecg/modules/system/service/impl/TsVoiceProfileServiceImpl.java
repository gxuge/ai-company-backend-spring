package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsVoiceProfileValidationAspect;
import org.jeecg.modules.aop.TsVoiceProfileValidationAspect.CheckTsVoiceProfileExists;
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
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVoConverter;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
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

    @Resource
    private TsVoiceProfileTagMapper tsVoiceProfileTagMapper;

    @Resource
    private TsVoiceTagMapper tsVoiceTagMapper;

    @Resource
    private TsUserVoiceConfigMapper tsUserVoiceConfigMapper;
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
    @CheckTsVoiceProfileExists(message = "音色不存在或已删除")
    public Result<?> deleteVoiceProfile(LoginUser user, Long id) {
        TsVoiceProfile profile = TsVoiceProfileValidationAspect.VOICE_PROFILE_CONTEXT.get();
        Long refCount = tsUserVoiceConfigMapper.countBySelectedVoiceProfileId(profile.getId());
        if (refCount != null && refCount > 0L) {
            throw new JeecgBootException("音色已被用户配置使用，无法删除");
        }
        profile.setStatus(0);
        profile.setUpdatedAt(new Date());
        this.updateById(profile);
        return Result.OK("删除成功");
    }
    @Override
    @CheckTsVoiceProfileExists(message = "音色不存在或已删除")
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
    @CheckTsVoiceProfileExists(message = "音色不存在或已删除")
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
                throw new JeecgBootException("标签不存在或参数非法");
            }
        }

        tsVoiceProfileTagMapper.deleteByVoiceProfileId(profile.getId());
        if (!tagIds.isEmpty()) {
            tsVoiceProfileTagMapper.insertBatch(profile.getId(), tagIds);
        }
        if (tagIds.isEmpty()) {
            return Result.OK("保存成功", new ArrayList<>());
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
        return Result.OK("保存成功", result);
    }
}
