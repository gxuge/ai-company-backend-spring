package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbSearchConfigSaveDto;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbSearchConfig;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbSearchConfigMapper;
import org.jeecg.modules.airag.kb.service.IKbSearchConfigService;
import org.jeecg.modules.airag.kb.vo.KbSearchConfigVo;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 知识库检索配置服务实现。
 */
@Service
public class KbSearchConfigServiceImpl extends ServiceImpl<KbSearchConfigMapper, KbSearchConfig> implements IKbSearchConfigService {
    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * 构造方法。
     *
     * @param kbBaseMapper 知识库主表Mapper
     */
    public KbSearchConfigServiceImpl(KbBaseMapper kbBaseMapper) {
        this.kbBaseMapper = kbBaseMapper;
    }

    /**
     * 创建或更新知识库检索配置。
     *
     * @param kbId 知识库ID
     * @param dto 配置请求
     * @return 配置返回对象
     */
    @Override
    public KbSearchConfigVo saveOrUpdateConfig(String kbId, KbSearchConfigSaveDto dto) {
        if (oConvertUtils.isEmpty(kbId)) {
            throw new JeecgBootException("知识库ID不能为空");
        }
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        KbSearchConfig entity = this.baseMapper.selectOne(new LambdaQueryWrapper<KbSearchConfig>().eq(KbSearchConfig::getKbId, kbId));
        Date now = new Date();
        if (entity == null) {
            entity = dto == null ? new KbSearchConfig() : dto.toEntity(kbId);
            entity.setKbId(kbId);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            this.save(entity);
            return KbSearchConfigVo.from(entity);
        }
        if (dto != null) {
            if (dto.getSearchMode() != null) {
                entity.setSearchMode(dto.getSearchMode());
            }
            if (dto.getSimilarityThreshold() != null) {
                entity.setSimilarityThreshold(dto.getSimilarityThreshold());
            }
            if (dto.getReferenceLimit() != null) {
                entity.setReferenceLimit(dto.getReferenceLimit());
            }
            if (dto.getTopK() != null) {
                entity.setTopK(dto.getTopK());
            }
            if (dto.getUseRerank() != null) {
                entity.setUseRerank(dto.getUseRerank());
            }
            if (dto.getUseQueryOptimization() != null) {
                entity.setUseQueryOptimization(dto.getUseQueryOptimization());
            }
            entity.setConfigJson(dto.buildConfigJson(entity.getConfigJson(), false));
        }
        entity.setUpdatedAt(now);
        this.updateById(entity);
        return KbSearchConfigVo.from(entity);
    }

    /**
     * 查询知识库检索配置，不存在时自动创建默认配置。
     *
     * @param kbId 知识库ID
     * @return 配置返回对象
     */
    @Override
    public KbSearchConfigVo getByKbId(String kbId) {
        if (oConvertUtils.isEmpty(kbId)) {
            throw new JeecgBootException("知识库ID不能为空");
        }
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        KbSearchConfig entity = this.baseMapper.selectOne(new LambdaQueryWrapper<KbSearchConfig>().eq(KbSearchConfig::getKbId, kbId));
        if (entity == null) {
            KbSearchConfigSaveDto dto = new KbSearchConfigSaveDto();
            return this.saveOrUpdateConfig(kbId, dto);
        }
        return KbSearchConfigVo.from(entity);
    }
}
