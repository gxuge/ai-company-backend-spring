package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbChunkCreateDto;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexSaveDto;
import org.jeecg.modules.airag.kb.dto.KbChunkQueryDto;
import org.jeecg.modules.airag.kb.dto.KbChunkUpdateDto;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.mapper.KbDocumentMapper;
import org.jeecg.modules.airag.kb.service.IKbChunkService;
import org.jeecg.modules.airag.kb.service.KbEmbeddingService;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;
import org.jeecg.modules.airag.kb.vo.KbChunkVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库chunk服务实现。
 */
@Service
public class KbChunkServiceImpl extends ServiceImpl<KbChunkMapper, KbChunk> implements IKbChunkService {
    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * 文档Mapper。
     */
    private final KbDocumentMapper kbDocumentMapper;

    /**
     * chunk索引Mapper。
     */
    private final KbChunkIndexMapper kbChunkIndexMapper;

    /**
     * embedding编排服务。
     */
    private final KbEmbeddingService kbEmbeddingService;

    /**
     * 构造方法。
     *
     * @param kbBaseMapper 知识库主表Mapper
     * @param kbDocumentMapper 文档Mapper
     * @param kbChunkIndexMapper chunk索引Mapper
     */
    public KbChunkServiceImpl(KbBaseMapper kbBaseMapper,
                              KbDocumentMapper kbDocumentMapper,
                              KbChunkIndexMapper kbChunkIndexMapper,
                              KbEmbeddingService kbEmbeddingService) {
        this.kbBaseMapper = kbBaseMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbChunkIndexMapper = kbChunkIndexMapper;
        this.kbEmbeddingService = kbEmbeddingService;
    }

    /**
     * 创建chunk并同步索引文本。
     *
     * @param kbId 知识库ID
     * @param dto 创建请求
     * @return chunk返回对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbChunkVo createChunk(String kbId, KbChunkCreateDto dto) {
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        if (KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("知识库已禁用，不能继续新增chunk");
        }
        KbDocument document = kbDocumentMapper.selectById(dto.getDocumentId());
        if (document == null || KbConstants.STATUS_DISABLE.equals(document.getStatus())) {
            throw new JeecgBootException("未找到对应文档");
        }
        if (!kbId.equals(document.getKbId())) {
            throw new JeecgBootException("文档不属于当前知识库");
        }

        KbChunk entity = dto.toEntity(kbId);
        Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);

        List<KbChunkIndex> indexList = new ArrayList<>();
        if (dto.getIndexList() == null || dto.getIndexList().isEmpty()) {
            KbChunkIndexSaveDto fallback = new KbChunkIndexSaveDto();
            fallback.setIndexText(dto.getContent());
            fallback.setIndexType(KbConstants.INDEX_TYPE_DEFAULT);
            fallback.setEmbeddingStatus(KbConstants.PROCESS_STATUS_PENDING);
            fallback.setSortNo(entity.getSortNo());
            fallback.setStatus(KbConstants.STATUS_ENABLE);
            indexList.add(fallback.toEntity(kbId, entity.getId()));
        } else {
            for (KbChunkIndexSaveDto indexDto : dto.getIndexList()) {
                KbChunkIndex indexEntity = indexDto.toEntity(kbId, entity.getId());
                indexEntity.setCreatedAt(now);
                indexEntity.setUpdatedAt(now);
                indexList.add(indexEntity);
            }
        }
        for (KbChunkIndex indexEntity : indexList) {
            kbChunkIndexMapper.insert(indexEntity);
        }
        kbEmbeddingService.refreshDocumentEmbedStatus(document.getId());
        KbChunkVo vo = KbChunkVo.from(entity);
        List<KbChunkIndexVo> indexVoList = new ArrayList<>();
        for (KbChunkIndex indexEntity : indexList) {
            indexVoList.add(KbChunkIndexVo.from(indexEntity));
        }
        vo.setIndexList(indexVoList);
        return vo;
    }

    /**
     * 更新chunk并重建索引文本。
     *
     * @param chunkId chunkID
     * @param dto 更新请求
     * @return chunk返回对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbChunkVo updateChunk(String chunkId, KbChunkUpdateDto dto) {
        KbChunk entity = this.getById(chunkId);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应chunk");
        }
        dto.applyTo(entity);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);

        Date now = new Date();
        kbChunkIndexMapper.update(null, new LambdaUpdateWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getChunkId, chunkId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .set(KbChunkIndex::getStatus, KbConstants.STATUS_DISABLE)
                .set(KbChunkIndex::getUpdatedAt, now));

        List<KbChunkIndex> indexList = new ArrayList<>();
        if (dto.getIndexList() == null || dto.getIndexList().isEmpty()) {
            KbChunkIndexSaveDto fallback = new KbChunkIndexSaveDto();
            fallback.setIndexText(entity.getContent());
            fallback.setIndexType(KbConstants.INDEX_TYPE_DEFAULT);
            fallback.setEmbeddingStatus(KbConstants.PROCESS_STATUS_PENDING);
            fallback.setSortNo(entity.getSortNo());
            fallback.setStatus(KbConstants.STATUS_ENABLE);
            indexList.add(fallback.toEntity(entity.getKbId(), entity.getId()));
        } else {
            for (KbChunkIndexSaveDto indexDto : dto.getIndexList()) {
                KbChunkIndex indexEntity = indexDto.toEntity(entity.getKbId(), entity.getId());
                indexEntity.setCreatedAt(now);
                indexEntity.setUpdatedAt(now);
                indexList.add(indexEntity);
            }
        }
        for (KbChunkIndex indexEntity : indexList) {
            kbChunkIndexMapper.insert(indexEntity);
        }
        kbEmbeddingService.deleteByChunkId(chunkId);
        kbEmbeddingService.refreshDocumentEmbedStatus(entity.getDocumentId());
        KbChunkVo vo = KbChunkVo.from(entity);
        List<KbChunkIndexVo> indexVoList = new ArrayList<>();
        for (KbChunkIndex indexEntity : indexList) {
            indexVoList.add(KbChunkIndexVo.from(indexEntity));
        }
        vo.setIndexList(indexVoList);
        return vo;
    }

    /**
     * 删除chunk并删除索引文本。
     *
     * @param chunkId chunkID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChunk(String chunkId) {
        KbChunk entity = this.getById(chunkId);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应chunk");
        }
        entity.setStatus(KbConstants.STATUS_DISABLE);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        Date now = new Date();
        kbChunkIndexMapper.update(null, new LambdaUpdateWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getChunkId, chunkId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .set(KbChunkIndex::getStatus, KbConstants.STATUS_DISABLE)
                .set(KbChunkIndex::getUpdatedAt, now));
        kbEmbeddingService.deleteByChunkId(chunkId);
        kbEmbeddingService.refreshDocumentEmbedStatus(entity.getDocumentId());
    }

    /**
     * 分页查询chunk。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    @Override
    public IPage<KbChunkVo> listChunks(String kbId, KbChunkQueryDto query) {
        int pageNo = query == null || query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        Page<KbChunk> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<KbChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbChunk::getKbId, kbId);
        wrapper.eq(KbChunk::getStatus, KbConstants.STATUS_ENABLE);
        if (query != null) {
            if (oConvertUtils.isNotEmpty(query.getDocumentId())) {
                wrapper.eq(KbChunk::getDocumentId, query.getDocumentId());
            }
            if (oConvertUtils.isNotEmpty(query.getChunkType())) {
                wrapper.eq(KbChunk::getChunkType, query.getChunkType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(KbChunk::getStatus, query.getStatus());
            }
        }
        wrapper.orderByAsc(KbChunk::getSortNo).orderByAsc(KbChunk::getCreatedAt);
        IPage<KbChunk> pageData = this.page(page, wrapper);

        List<String> chunkIds = new ArrayList<>();
        for (KbChunk chunk : pageData.getRecords()) {
            chunkIds.add(chunk.getId());
        }
        Map<String, List<KbChunkIndexVo>> indexGroup = new java.util.HashMap<>();
        if (!chunkIds.isEmpty()) {
            List<KbChunkIndex> indexEntities = kbChunkIndexMapper.selectList(new LambdaQueryWrapper<KbChunkIndex>()
                    .in(KbChunkIndex::getChunkId, chunkIds)
                    .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                    .orderByAsc(KbChunkIndex::getSortNo)
                    .orderByAsc(KbChunkIndex::getCreatedAt));
            indexGroup = indexEntities.stream()
                    .map(KbChunkIndexVo::from)
                    .collect(Collectors.groupingBy(KbChunkIndexVo::getChunkId));
        }

        Page<KbChunkVo> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<KbChunkVo> records = new ArrayList<>();
        for (KbChunk chunk : pageData.getRecords()) {
            KbChunkVo vo = KbChunkVo.from(chunk);
            List<KbChunkIndexVo> indexVoList = indexGroup.get(chunk.getId());
            if (indexVoList != null) {
                vo.setIndexList(indexVoList);
            }
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }
}
