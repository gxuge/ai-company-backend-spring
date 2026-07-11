package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexQueryDto;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexSaveDto;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.service.IKbChunkIndexService;
import org.jeecg.modules.airag.kb.service.KbEmbeddingService;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * chunk索引服务实现。
 */
@Service
public class KbChunkIndexServiceImpl extends ServiceImpl<KbChunkIndexMapper, KbChunkIndex> implements IKbChunkIndexService {
    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * chunk Mapper。
     */
    private final KbChunkMapper kbChunkMapper;

    /**
     * embedding编排服务。
     */
    private final KbEmbeddingService kbEmbeddingService;

    /**
     * 构造方法。
     *
     * @param kbBaseMapper 知识库主表Mapper
     * @param kbChunkMapper chunk Mapper
     */
    public KbChunkIndexServiceImpl(KbBaseMapper kbBaseMapper, KbChunkMapper kbChunkMapper, KbEmbeddingService kbEmbeddingService) {
        this.kbBaseMapper = kbBaseMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbEmbeddingService = kbEmbeddingService;
    }

    /**
     * 创建chunk索引。
     *
     * @param kbId 知识库ID
     * @param chunkId 分段ID
     * @param dto 保存请求
     * @return 返回对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbChunkIndexVo createIndex(String kbId, String chunkId, KbChunkIndexSaveDto dto) {
        return saveSingleIndex(kbId, chunkId, dto, null);
    }

    /**
     * 批量创建chunk索引。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dtoList 保存请求列表
     * @return 返回对象列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KbChunkIndexVo> createIndexes(String kbId, String chunkId, List<KbChunkIndexSaveDto> dtoList) {
        List<KbChunkIndexVo> result = new ArrayList<>();
        if (dtoList == null || dtoList.isEmpty()) {
            return result;
        }
        for (KbChunkIndexSaveDto dto : dtoList) {
            result.add(saveSingleIndex(kbId, chunkId, dto, null));
        }
        return result;
    }

    /**
     * 更新chunk索引。
     *
     * @param indexId 索引ID
     * @param dto 保存请求
     * @return 返回对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbChunkIndexVo updateIndex(String indexId, KbChunkIndexSaveDto dto) {
        KbChunkIndex entity = this.getById(indexId);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应chunk索引");
        }
        KbChunk chunk = kbChunkMapper.selectById(entity.getChunkId());
        if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus()) || !entity.getKbId().equals(chunk.getKbId())) {
            throw new JeecgBootException("未找到对应chunk索引");
        }
        if (dto.getIndexText() != null) {
            validateDuplicateIndexText(entity.getChunkId(), entity.getId(), dto.getIndexText());
        }
        if (dto.getIndexText() != null) {
            entity.setIndexText(dto.getIndexText());
        }
        if (dto.getIndexType() != null) {
            entity.setIndexType(dto.getIndexType());
        }
        if (dto.getEmbeddingStatus() != null) {
            entity.setEmbeddingStatus(dto.getEmbeddingStatus());
        }
        if (dto.getSortNo() != null) {
            entity.setSortNo(dto.getSortNo());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getMetadataJson() != null) {
            entity.setMetadataJson(dto.getMetadataJson());
        }
        boolean needRebuild = dto.getIndexText() != null || dto.getIndexType() != null || dto.getMetadataJson() != null;
        if (needRebuild) {
            entity.setEmbeddingStatus(KbConstants.PROCESS_STATUS_PENDING);
            kbEmbeddingService.deleteByIndexId(indexId);
        }
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        kbEmbeddingService.refreshDocumentEmbedStatus(chunk == null ? null : chunk.getDocumentId());
        return KbChunkIndexVo.from(entity);
    }

    /**
     * 删除chunk索引。
     *
     * @param indexId 索引ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIndex(String indexId) {
        KbChunkIndex entity = this.getById(indexId);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应chunk索引");
        }
        entity.setStatus(KbConstants.STATUS_DISABLE);
        entity.setUpdatedAt(new Date());
        this.updateById(entity);
        kbEmbeddingService.deleteByIndexId(indexId);
        KbChunk chunk = kbChunkMapper.selectById(entity.getChunkId());
        if (chunk != null) {
            kbEmbeddingService.refreshDocumentEmbedStatus(chunk.getDocumentId());
        }
    }

    /**
     * 查询chunk索引详情。
     *
     * @param indexId 索引ID
     * @return 返回对象
     */
    @Override
    public KbChunkIndexVo getIndex(String indexId) {
        KbChunkIndex entity = this.getById(indexId);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应chunk索引");
        }
        KbChunk chunk = kbChunkMapper.selectById(entity.getChunkId());
        if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus()) || !entity.getKbId().equals(chunk.getKbId())) {
            throw new JeecgBootException("未找到对应chunk索引");
        }
        return KbChunkIndexVo.from(entity);
    }

    /**
     * 分页查询chunk索引列表。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    @Override
    public IPage<KbChunkIndexVo> listIndexes(String kbId, KbChunkIndexQueryDto query) {
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        int pageNo = query == null || query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        Page<KbChunkIndex> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<KbChunkIndex> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbChunkIndex::getKbId, kbId);
        wrapper.eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE);
        if (query != null) {
            if (oConvertUtils.isNotEmpty(query.getChunkId())) {
                wrapper.eq(KbChunkIndex::getChunkId, query.getChunkId());
            }
            if (oConvertUtils.isNotEmpty(query.getIndexType())) {
                wrapper.eq(KbChunkIndex::getIndexType, query.getIndexType());
            }
            if (oConvertUtils.isNotEmpty(query.getEmbeddingStatus())) {
                wrapper.eq(KbChunkIndex::getEmbeddingStatus, query.getEmbeddingStatus());
            }
            if (query.getStatus() != null) {
                wrapper.eq(KbChunkIndex::getStatus, query.getStatus());
            }
        }
        wrapper.orderByAsc(KbChunkIndex::getSortNo).orderByAsc(KbChunkIndex::getCreatedAt);
        IPage<KbChunkIndex> pageData = this.page(page, wrapper);
        Page<KbChunkIndexVo> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<KbChunkIndexVo> records = new ArrayList<>();
        for (KbChunkIndex entity : pageData.getRecords()) {
            records.add(KbChunkIndexVo.from(entity));
        }
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 保存单条chunk索引。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dto 保存请求
     * @param currentIndexId 当前索引ID
     * @return 返回对象
     */
    private KbChunkIndexVo saveSingleIndex(String kbId, String chunkId, KbChunkIndexSaveDto dto, String currentIndexId) {
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        KbChunk chunk = kbChunkMapper.selectById(chunkId);
        if (chunk == null || KbConstants.STATUS_DISABLE.equals(chunk.getStatus())) {
            throw new JeecgBootException("未找到对应chunk");
        }
        if (!kbId.equals(chunk.getKbId())) {
            throw new JeecgBootException("chunk不属于当前知识库");
        }
        validateDuplicateIndexText(chunkId, currentIndexId, dto.getIndexText());
        KbChunkIndex entity = dto.toEntity(kbId, chunkId);
        Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);
        kbEmbeddingService.refreshDocumentEmbedStatus(chunk.getDocumentId());
        return KbChunkIndexVo.from(entity);
    }

    /**
     * 检查同一chunk下索引文本重复。
     *
     * @param chunkId chunk ID
     * @param currentIndexId 当前索引ID
     * @param indexText 索引文本
     */
    private void validateDuplicateIndexText(String chunkId, String currentIndexId, String indexText) {
        if (oConvertUtils.isEmpty(indexText)) {
            throw new JeecgBootException("索引文本不能为空");
        }
        String normalized = indexText.trim();
        LambdaQueryWrapper<KbChunkIndex> wrapper = new LambdaQueryWrapper<KbChunkIndex>()
                .eq(KbChunkIndex::getChunkId, chunkId)
                .eq(KbChunkIndex::getStatus, KbConstants.STATUS_ENABLE)
                .eq(KbChunkIndex::getIndexText, normalized);
        if (oConvertUtils.isNotEmpty(currentIndexId)) {
            wrapper.ne(KbChunkIndex::getId, currentIndexId);
        }
        Long count = this.baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new JeecgBootException("同一chunk下索引文本不能重复");
        }
    }
}
