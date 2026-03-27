package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.aop.TsVoiceTagValidationAspect;
import org.jeecg.modules.aop.TsVoiceTagValidationAspect.CheckTsVoiceTagExists;
import org.jeecg.modules.system.dto.tsvoicetag.TsVoiceTagQueryDto;
import org.jeecg.modules.system.dto.tsvoicetag.TsVoiceTagSaveDto;
import org.jeecg.modules.system.entity.TsVoiceTag;
import org.jeecg.modules.system.mapper.TsVoiceTagMapper;
import org.jeecg.modules.system.po.tsvoicetag.TsVoiceTagQueryPo;
import org.jeecg.modules.system.po.tsvoicetag.TsVoiceTagSavePo;
import org.jeecg.modules.system.service.ITsVoiceTagService;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVoConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TsVoiceTagServiceImpl extends ServiceImpl<TsVoiceTagMapper, TsVoiceTag>
        implements ITsVoiceTagService {
    @Override
    public Result<Page<TsVoiceTagVo>> pageVoiceTags(LoginUser user, TsVoiceTagQueryDto request) {
        TsVoiceTagQueryPo queryPo = TsVoiceTagQueryPo.fromRequest(request);
        Page<TsVoiceTag> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsVoiceTag> pageData = baseMapper.selectTagPage(page, queryPo);
        return Result.OK(TsVoiceTagVoConverter.fromPage(pageData));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsVoiceTagVo> addVoiceTag(LoginUser user, TsVoiceTagSaveDto request) {
        TsVoiceTagSavePo savePo = TsVoiceTagSavePo.fromRequest(request);
        if (savePo.getTagName() == null) {
            throw new JeecgBootException("标签名称不能为空");
        }

        Long count = baseMapper.countByTagName(savePo.getTagName());
        if (count != null && count > 0L) {
            throw new JeecgBootException("标签名称已存在");
        }

        TsVoiceTag tag = new TsVoiceTag();
        savePo.applyTo(tag);
        this.save(tag);
        return Result.OK("创建成功", TsVoiceTagVoConverter.fromEntity(tag));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CheckTsVoiceTagExists(message = "标签不存在")
    public Result<?> deleteVoiceTag(LoginUser user, Long id) {
        TsVoiceTag tag = TsVoiceTagValidationAspect.VOICE_TAG_CONTEXT.get();
        Long relCount = baseMapper.countProfileRelByTagId(tag.getId());
        if (relCount != null && relCount > 0L) {
            throw new JeecgBootException("标签已被音色使用，无法删除");
        }
        this.removeById(tag.getId());
        return Result.OK("删除成功");
    }
}
