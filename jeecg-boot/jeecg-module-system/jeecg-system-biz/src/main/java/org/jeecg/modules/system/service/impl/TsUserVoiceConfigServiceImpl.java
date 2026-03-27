package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsVoiceProfileValidationAspect.CheckTsVoiceProfileExists;
import org.jeecg.modules.system.dto.tsuservoiceconfig.TsUserVoiceConfigSaveDto;
import org.jeecg.modules.system.entity.TsUserVoiceConfig;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.entity.TsVoiceProfileTag;
import org.jeecg.modules.system.entity.TsVoiceTag;
import org.jeecg.modules.system.mapper.TsUserVoiceConfigMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileTagMapper;
import org.jeecg.modules.system.mapper.TsVoiceTagMapper;
import org.jeecg.modules.system.po.tsuservoiceconfig.TsUserVoiceConfigSavePo;
import org.jeecg.modules.system.service.ITsUserVoiceConfigService;
import org.jeecg.modules.system.vo.tsuservoiceconfig.TsUserVoiceConfigVo;
import org.jeecg.modules.system.vo.tsuservoiceconfig.TsUserVoiceConfigVoConverter;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVoConverter;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVoConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class TsUserVoiceConfigServiceImpl extends ServiceImpl<TsUserVoiceConfigMapper, TsUserVoiceConfig>
        implements ITsUserVoiceConfigService {

    @Resource
    private TsVoiceProfileMapper tsVoiceProfileMapper;

    @Resource
    private TsVoiceProfileTagMapper tsVoiceProfileTagMapper;

    @Resource
    private TsVoiceTagMapper tsVoiceTagMapper;
    @Override
    public Result<TsUserVoiceConfigVo> getCurrentConfig(LoginUser user) {
        String userId = user.getId();

        TsUserVoiceConfig config = baseMapper.selectByUserId(userId);
        if (config == null) {
            return Result.OK(TsUserVoiceConfigVoConverter.fromDefault(userId));
        }

        TsVoiceProfileVo selectedVoiceProfile = null;
        if (config.getSelectedVoiceProfileId() != null) {
            TsVoiceProfile profile = tsVoiceProfileMapper.selectActiveById(config.getSelectedVoiceProfileId());
            if (profile != null) {
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
                Map<Long, TsVoiceTagVo> tagVoMap = new HashMap<>();
                if (!tagIds.isEmpty()) {
                    List<TsVoiceTag> tags = tsVoiceTagMapper.selectBatchIds(tagIds);
                    if (tags != null) {
                        for (TsVoiceTag tag : tags) {
                            if (tag != null && tag.getId() != null) {
                                tagVoMap.put(tag.getId(), TsVoiceTagVoConverter.fromEntity(tag));
                            }
                        }
                    }
                }
                List<TsVoiceTagVo> tagVos = new ArrayList<>();
                for (Long tagId : tagIds) {
                    TsVoiceTagVo tagVo = tagVoMap.get(tagId);
                    if (tagVo != null) {
                        tagVos.add(tagVo);
                    }
                }
                selectedVoiceProfile = TsVoiceProfileVoConverter.fromEntity(profile, tagIds, tagVos);
            }
        }
        return Result.OK(TsUserVoiceConfigVoConverter.fromEntity(config, selectedVoiceProfile));
    }
    @Override
    @CheckTsVoiceProfileExists(message = "所选音色不存在")
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserVoiceConfigVo> saveCurrentConfig(LoginUser user, TsUserVoiceConfigSaveDto request) {
        String userId = user.getId();

        request.applyDefaults();
        TsUserVoiceConfigSavePo savePo = TsUserVoiceConfigSavePo.fromRequest(request);

        Date now = new Date();
        TsUserVoiceConfig config = baseMapper.selectByUserId(userId);
        if (config == null) {
            config = new TsUserVoiceConfig();
            config.setUserId(userId);
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            savePo.applyTo(config);
            try {
                this.save(config);
            } catch (DuplicateKeyException ex) {
                TsUserVoiceConfig existed = baseMapper.selectByUserId(userId);
                if (existed == null) {
                    throw ex;
                }
                savePo.applyTo(existed);
                existed.setUpdatedAt(now);
                this.updateById(existed);
                config = existed;
            }
        } else {
            savePo.applyTo(config);
            config.setUpdatedAt(now);
            this.updateById(config);
        }

        TsVoiceProfileVo selectedVoiceProfile = null;
        if (config.getSelectedVoiceProfileId() != null) {
            TsVoiceProfile profile = tsVoiceProfileMapper.selectActiveById(config.getSelectedVoiceProfileId());
            if (profile != null) {
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
                Map<Long, TsVoiceTagVo> tagVoMap = new HashMap<>();
                if (!tagIds.isEmpty()) {
                    List<TsVoiceTag> tags = tsVoiceTagMapper.selectBatchIds(tagIds);
                    if (tags != null) {
                        for (TsVoiceTag tag : tags) {
                            if (tag != null && tag.getId() != null) {
                                tagVoMap.put(tag.getId(), TsVoiceTagVoConverter.fromEntity(tag));
                            }
                        }
                    }
                }
                List<TsVoiceTagVo> tagVos = new ArrayList<>();
                for (Long tagId : tagIds) {
                    TsVoiceTagVo tagVo = tagVoMap.get(tagId);
                    if (tagVo != null) {
                        tagVos.add(tagVo);
                    }
                }
                selectedVoiceProfile = TsVoiceProfileVoConverter.fromEntity(profile, tagIds, tagVos);
            }
        }
        return Result.OK("保存成功", TsUserVoiceConfigVoConverter.fromEntity(config, selectedVoiceProfile));
    }
}
