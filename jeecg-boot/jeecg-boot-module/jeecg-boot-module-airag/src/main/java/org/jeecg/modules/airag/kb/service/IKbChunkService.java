package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.kb.dto.KbChunkCreateDto;
import org.jeecg.modules.airag.kb.dto.KbChunkQueryDto;
import org.jeecg.modules.airag.kb.dto.KbChunkUpdateDto;
import org.jeecg.modules.airag.kb.entity.KbChunk;
import org.jeecg.modules.airag.kb.vo.KbChunkVo;

/**
 * 知识库chunk服务。
 */
public interface IKbChunkService extends IService<KbChunk> {
    /**
     * 创建chunk并同步索引文本。
     *
     * @param kbId 知识库ID
     * @param dto 创建请求
     * @return chunk返回对象
     */
    KbChunkVo createChunk(String kbId, KbChunkCreateDto dto);

    /**
     * 更新chunk并重建索引文本。
     *
     * @param chunkId chunkID
     * @param dto 更新请求
     * @return chunk返回对象
     */
    KbChunkVo updateChunk(String chunkId, KbChunkUpdateDto dto);

    /**
     * 删除chunk并删除索引文本。
     *
     * @param chunkId chunkID
     */
    void deleteChunk(String chunkId);

    /**
     * 分页查询chunk。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    IPage<KbChunkVo> listChunks(String kbId, KbChunkQueryDto query);
}
