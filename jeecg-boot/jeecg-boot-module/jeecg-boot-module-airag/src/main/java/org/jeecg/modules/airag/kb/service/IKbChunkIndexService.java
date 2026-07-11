package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexQueryDto;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexSaveDto;
import org.jeecg.modules.airag.kb.entity.KbChunkIndex;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;

import java.util.List;

/**
 * chunk索引服务。
 */
public interface IKbChunkIndexService extends IService<KbChunkIndex> {
    /**
     * 创建chunk索引。
     *
     * @param kbId 知识库ID
     * @param chunkId 分段ID
     * @param dto 保存请求
     * @return 返回对象
     */
    KbChunkIndexVo createIndex(String kbId, String chunkId, KbChunkIndexSaveDto dto);

    /**
     * 批量创建chunk索引。
     *
     * @param kbId 知识库ID
     * @param chunkId 分段ID
     * @param dtoList 保存请求列表
     * @return 返回对象列表
     */
    List<KbChunkIndexVo> createIndexes(String kbId, String chunkId, List<KbChunkIndexSaveDto> dtoList);

    /**
     * 更新chunk索引。
     *
     * @param indexId 索引ID
     * @param dto 保存请求
     * @return 返回对象
     */
    KbChunkIndexVo updateIndex(String indexId, KbChunkIndexSaveDto dto);

    /**
     * 删除chunk索引。
     *
     * @param indexId 索引ID
     */
    void deleteIndex(String indexId);

    /**
     * 查询chunk索引详情。
     *
     * @param indexId 索引ID
     * @return 返回对象
     */
    KbChunkIndexVo getIndex(String indexId);

    /**
     * 分页查询chunk索引列表。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    IPage<KbChunkIndexVo> listIndexes(String kbId, KbChunkIndexQueryDto query);
}
