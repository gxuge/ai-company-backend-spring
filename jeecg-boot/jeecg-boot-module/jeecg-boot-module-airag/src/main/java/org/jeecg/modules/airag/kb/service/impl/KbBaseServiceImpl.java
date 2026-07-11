package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbBaseCreateDto;
import org.jeecg.modules.airag.kb.dto.KbBaseQueryDto;
import org.jeecg.modules.airag.kb.dto.KbBaseUpdateDto;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.entity.KbSearchConfig;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.mapper.KbDocumentMapper;
import org.jeecg.modules.airag.kb.mapper.KbSearchConfigMapper;
import org.jeecg.modules.airag.kb.service.IKbBaseService;
import org.jeecg.modules.airag.kb.service.KbEmbeddingService;
import org.jeecg.modules.airag.kb.vo.KbBaseVo;
import org.jeecg.modules.airag.kb.vo.KbSearchConfigVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 知识库主表服务实现。
 */
@Service
public class KbBaseServiceImpl extends ServiceImpl<KbBaseMapper, KbBase> implements IKbBaseService {
    /**
     * 文档Mapper。
     */
    private final KbDocumentMapper kbDocumentMapper;

    /**
     * chunk Mapper。
     */
    private final KbChunkMapper kbChunkMapper;

    /**
     * chunk索引Mapper。
     */
    private final KbChunkIndexMapper kbChunkIndexMapper;

    /**
     * 检索配置Mapper。
     */
    private final KbSearchConfigMapper kbSearchConfigMapper;

    /**
     * embedding编排服务。
     */
    private final KbEmbeddingService kbEmbeddingService;

    /**
     * 构造方法。
     *
     * @param kbDocumentMapper 文档Mapper
     * @param kbChunkMapper chunk Mapper
     * @param kbChunkIndexMapper chunk索引Mapper
     * @param kbSearchConfigMapper 检索配置Mapper
     */
    public KbBaseServiceImpl(KbDocumentMapper kbDocumentMapper,
                             KbChunkMapper kbChunkMapper,
                             KbChunkIndexMapper kbChunkIndexMapper,
                             KbSearchConfigMapper kbSearchConfigMapper,
                             KbEmbeddingService kbEmbeddingService) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbChunkIndexMapper = kbChunkIndexMapper;
        this.kbSearchConfigMapper = kbSearchConfigMapper;
        this.kbEmbeddingService = kbEmbeddingService;
    }

    /**
     * 创建知识库并自动创建默认检索配置。
     *
     * @param dto 创建请求
     * @return 知识库返回对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbBaseVo createKb(KbBaseCreateDto dto) {
        KbBase entity = dto.toEntity();
        Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);

        KbSearchConfig config = new KbSearchConfig();
        config.setKbId(entity.getId());
        config.setSearchMode(KbConstants.DEFAULT_SEARCH_MODE);
        config.setSimilarityThreshold(KbConstants.DEFAULT_SIMILARITY_THRESHOLD);
        config.setReferenceLimit(KbConstants.DEFAULT_REFERENCE_LIMIT);
        config.setTopK(KbConstants.DEFAULT_TOP_K);
        config.setUseRerank(KbConstants.DEFAULT_USE_RERANK);
        config.setUseQueryOptimization(KbConstants.DEFAULT_USE_QUERY_OPTIMIZATION);
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        kbSearchConfigMapper.insert(config);

        KbBaseVo vo = KbBaseVo.from(entity);
        vo.setSearchConfig(KbSearchConfigVo.from(config));
        return vo;
    }

    /**
     * 更新知识库。
     *
     * @param id 知识库ID
     * @param dto 更新请求
     * @return 知识库返回对象
     */
    @Override
    public KbBaseVo updateKb(String id, KbBaseUpdateDto dto) {
        KbBase entity = this.getById(id);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        dto.applyTo(entity);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        KbSearchConfig config = kbSearchConfigMapper.selectOne(new LambdaQueryWrapper<KbSearchConfig>().eq(KbSearchConfig::getKbId, id));
        KbBaseVo vo = KbBaseVo.from(entity);
        vo.setSearchConfig(KbSearchConfigVo.from(config));
        return vo;
    }

    /**
     * 删除知识库并级联禁用子数据。
     *
     * @param id 知识库ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKb(String id) {
        KbBase entity = this.getById(id);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        Date now = new Date();
        entity.setStatus(KbConstants.STATUS_DISABLE);
        entity.setUpdatedAt(now);
        this.updateById(entity);

        List<KbDocument> documentList = kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getKbId, id));
        if (!documentList.isEmpty()) {
            for (KbDocument document : documentList) {
                document.setStatus(KbConstants.STATUS_DISABLE);
                document.setUpdatedAt(now);
                kbDocumentMapper.updateById(document);
            }
        }

        List<KbChunk> chunkList = kbChunkMapper.selectList(new LambdaQueryWrapper<KbChunk>().eq(KbChunk::getKbId, id));
        if (!chunkList.isEmpty()) {
            List<String> chunkIds = new ArrayList<>();
            for (KbChunk chunk : chunkList) {
                chunk.setStatus(KbConstants.STATUS_DISABLE);
                chunk.setUpdatedAt(now);
                kbChunkMapper.updateById(chunk);
                chunkIds.add(chunk.getId());
            }
            if (!chunkIds.isEmpty()) {
                kbChunkIndexMapper.update(null, new LambdaUpdateWrapper<KbChunkIndex>()
                        .in(KbChunkIndex::getChunkId, chunkIds)
                        .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                        .set(KbChunkIndex::getStatus, KbConstants.STATUS_DISABLE)
                        .set(KbChunkIndex::getUpdatedAt, now));
            }
        }
        kbEmbeddingService.deleteByKbId(id);
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库ID
     * @return 知识库返回对象
     */
    @Override
    public KbBaseVo getKb(String id) {
        KbBase entity = this.getById(id);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        KbSearchConfig config = kbSearchConfigMapper.selectOne(new LambdaQueryWrapper<KbSearchConfig>().eq(KbSearchConfig::getKbId, id));
        if (config == null) {
            KbSearchConfig defaultConfig = new KbSearchConfig();
            defaultConfig.setKbId(id);
            defaultConfig.setSearchMode(KbConstants.DEFAULT_SEARCH_MODE);
            defaultConfig.setSimilarityThreshold(KbConstants.DEFAULT_SIMILARITY_THRESHOLD);
            defaultConfig.setReferenceLimit(KbConstants.DEFAULT_REFERENCE_LIMIT);
            defaultConfig.setTopK(KbConstants.DEFAULT_TOP_K);
            defaultConfig.setUseRerank(KbConstants.DEFAULT_USE_RERANK);
            defaultConfig.setUseQueryOptimization(KbConstants.DEFAULT_USE_QUERY_OPTIMIZATION);
            Date now = new Date();
            defaultConfig.setCreatedAt(now);
            defaultConfig.setUpdatedAt(now);
            kbSearchConfigMapper.insert(defaultConfig);
            config = defaultConfig;
        }
        KbBaseVo vo = KbBaseVo.from(entity);
        vo.setSearchConfig(KbSearchConfigVo.from(config));
        return vo;
    }

    /**
     * 分页查询知识库列表。
     *
     * @param query 查询请求
     * @return 分页结果
     */
    @Override
    public IPage<KbBaseVo> listKb(KbBaseQueryDto query) {
        int pageNo = query == null || query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        Page<KbBase> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<KbBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbBase::getStatus, KbConstants.STATUS_ENABLE);
        if (query != null) {
            if (oConvertUtils.isNotEmpty(query.getName())) {
                wrapper.like(KbBase::getName, query.getName());
            }
            if (oConvertUtils.isNotEmpty(query.getBizType())) {
                wrapper.eq(KbBase::getBizType, query.getBizType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(KbBase::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(KbBase::getUpdatedAt).orderByDesc(KbBase::getCreatedAt);
        IPage<KbBase> pageData = this.page(page, wrapper);
        Page<KbBaseVo> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<KbBaseVo> records = new ArrayList<>();
        for (KbBase entity : pageData.getRecords()) {
            records.add(KbBaseVo.from(entity));
        }
        voPage.setRecords(records);
        return voPage;
    }
}
