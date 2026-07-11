package org.jeecg.modules.airag.kb.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.entity.KbVectorRecord;
import org.jeecg.modules.airag.kb.mapper.KbVectorRecordMapper;
import org.jeecg.modules.airag.kb.service.VectorStoreService;
import org.jeecg.modules.airag.kb.vo.VectorStoreMetadataVO;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 向量库服务实现。
 */
@Service
public class VectorStoreServiceImpl extends ServiceImpl<KbVectorRecordMapper, KbVectorRecord> implements VectorStoreService {
    /**
     * 写入或覆盖向量。
     *
     * @param vectorId 向量ID
     * @param vector 向量值
     * @param metadata 元数据
     */
    @Override
    public void upsert(String vectorId, List<Float> vector, VectorStoreMetadataVO metadata) {
        if (oConvertUtils.isEmpty(vectorId)) {
            throw new JeecgBootException("vectorId不能为空");
        }
        if (vector == null || vector.isEmpty()) {
            throw new JeecgBootException("向量不能为空");
        }
        delete(vectorId);
        KbVectorRecord record = new KbVectorRecord();
        Date now = new Date();
        record.setVectorId(vectorId);
        record.setKbId(metadata.getKbId());
        record.setDocumentId(metadata.getDocumentId());
        record.setChunkId(metadata.getChunkId());
        record.setChunkIndexId(metadata.getChunkIndexId());
        record.setEmbeddingModel(metadata.getEmbeddingModel());
        record.setVectorDimension(metadata.getVectorDimension());
        record.setEmbeddingDurationMs(metadata.getEmbeddingDurationMs());
        record.setContentPreview(metadata.getContentPreview());
        record.setVectorJson(JSON.toJSONString(vector));
        record.setMetadataJson(metadata.getMetadataJson());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        this.save(record);
    }

    /**
     * 删除单个向量。
     *
     * @param vectorId 向量ID
     */
    @Override
    public void delete(String vectorId) {
        if (oConvertUtils.isEmpty(vectorId)) {
            return;
        }
        this.remove(new LambdaQueryWrapper<KbVectorRecord>().eq(KbVectorRecord::getVectorId, vectorId));
    }

    /**
     * 按知识库删除向量。
     *
     * @param kbId 知识库ID
     */
    @Override
    public void deleteByKbId(String kbId) {
        if (oConvertUtils.isEmpty(kbId)) {
            return;
        }
        this.remove(new LambdaUpdateWrapper<KbVectorRecord>().eq(KbVectorRecord::getKbId, kbId));
    }

    /**
     * 按文档删除向量。
     *
     * @param documentId 文档ID
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        if (oConvertUtils.isEmpty(documentId)) {
            return;
        }
        this.remove(new LambdaUpdateWrapper<KbVectorRecord>().eq(KbVectorRecord::getDocumentId, documentId));
    }

    /**
     * 按chunk删除向量。
     *
     * @param chunkId chunk ID
     */
    @Override
    public void deleteByChunkId(String chunkId) {
        if (oConvertUtils.isEmpty(chunkId)) {
            return;
        }
        this.remove(new LambdaUpdateWrapper<KbVectorRecord>().eq(KbVectorRecord::getChunkId, chunkId));
    }

    /**
     * 按chunk_index删除向量。
     *
     * @param chunkIndexId chunk_index ID
     */
    @Override
    public void deleteByChunkIndexId(String chunkIndexId) {
        if (oConvertUtils.isEmpty(chunkIndexId)) {
            return;
        }
        this.remove(new LambdaUpdateWrapper<KbVectorRecord>().eq(KbVectorRecord::getChunkIndexId, chunkIndexId));
    }
}
