package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.consts.KbConstants;
import org.jeecg.modules.airag.kb.dto.KbDocumentCreateDto;
import org.jeecg.modules.airag.kb.dto.KbDocumentQueryDto;
import org.jeecg.modules.airag.kb.entity.KbBase;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.mapper.KbBaseMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkIndexMapper;
import org.jeecg.modules.airag.kb.mapper.KbChunkMapper;
import org.jeecg.modules.airag.kb.mapper.KbDocumentMapper;
import org.jeecg.modules.airag.kb.service.IKbDocumentService;
import org.jeecg.modules.airag.kb.service.KbEmbeddingService;
import org.jeecg.modules.airag.kb.vo.KbDocumentVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 知识库文档服务实现。
 */
@Service
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument> implements IKbDocumentService {
    /**
     * 知识库主表Mapper。
     */
    private final KbBaseMapper kbBaseMapper;

    /**
     * chunk Mapper。
     */
    private final KbChunkMapper kbChunkMapper;

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
     * @param kbChunkMapper chunk Mapper
     * @param kbChunkIndexMapper chunk索引Mapper
     */
    public KbDocumentServiceImpl(KbBaseMapper kbBaseMapper,
                                 KbChunkMapper kbChunkMapper,
                                 KbChunkIndexMapper kbChunkIndexMapper,
                                 KbEmbeddingService kbEmbeddingService) {
        this.kbBaseMapper = kbBaseMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.kbChunkIndexMapper = kbChunkIndexMapper;
        this.kbEmbeddingService = kbEmbeddingService;
    }

    /**
     * 创建文档记录。
     *
     * @param kbId 知识库ID
     * @param dto 创建请求
     * @return 文档返回对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocumentVo createDocument(String kbId, KbDocumentCreateDto dto) {
        KbBase kb = kbBaseMapper.selectById(kbId);
        if (kb == null || KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("未找到对应知识库");
        }
        if (KbConstants.STATUS_DISABLE.equals(kb.getStatus())) {
            throw new JeecgBootException("知识库已禁用，不能继续新增文档");
        }
        KbDocument entity = dto.toEntity(kbId);
        Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);
        return KbDocumentVo.from(entity);
    }

    /**
     * 分页查询文档。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    @Override
    public IPage<KbDocumentVo> listDocuments(String kbId, KbDocumentQueryDto query) {
        int pageNo = query == null || query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        Page<KbDocument> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbDocument::getKbId, kbId);
        wrapper.eq(KbDocument::getStatus, KbConstants.STATUS_ENABLE);
        if (query != null) {
            if (oConvertUtils.isNotEmpty(query.getName())) {
                wrapper.like(KbDocument::getName, query.getName());
            }
            if (oConvertUtils.isNotEmpty(query.getSourceType())) {
                wrapper.eq(KbDocument::getSourceType, query.getSourceType());
            }
            if (oConvertUtils.isNotEmpty(query.getParseStatus())) {
                wrapper.eq(KbDocument::getParseStatus, query.getParseStatus());
            }
            if (oConvertUtils.isNotEmpty(query.getChunkStatus())) {
                wrapper.eq(KbDocument::getChunkStatus, query.getChunkStatus());
            }
            if (oConvertUtils.isNotEmpty(query.getEmbedStatus())) {
                wrapper.eq(KbDocument::getEmbedStatus, query.getEmbedStatus());
            }
            if (query.getStatus() != null) {
                wrapper.eq(KbDocument::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(KbDocument::getUpdatedAt).orderByDesc(KbDocument::getCreatedAt);
        IPage<KbDocument> pageData = this.page(page, wrapper);
        Page<KbDocumentVo> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<KbDocumentVo> records = new ArrayList<>();
        for (KbDocument entity : pageData.getRecords()) {
            records.add(KbDocumentVo.from(entity));
        }
        voPage.setRecords(records);
        return voPage;
    }

    /**
     * 删除文档并级联禁用chunk。
     *
     * @param documentId 文档ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String documentId) {
        KbDocument entity = this.getById(documentId);
        if (entity == null || KbConstants.STATUS_DISABLE.equals(entity.getStatus())) {
            throw new JeecgBootException("未找到对应文档");
        }
        Date now = new Date();
        entity.setStatus(KbConstants.STATUS_DISABLE);
        entity.setUpdatedAt(now);
        this.updateById(entity);

        List<KbChunk> chunkList = kbChunkMapper.selectList(new LambdaQueryWrapper<KbChunk>().eq(KbChunk::getDocumentId, documentId));
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
        kbEmbeddingService.deleteByDocumentId(documentId);
    }
}
