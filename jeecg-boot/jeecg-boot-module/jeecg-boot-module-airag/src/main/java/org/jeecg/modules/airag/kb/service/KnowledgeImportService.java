package org.jeecg.modules.airag.kb.service;

import org.jeecg.modules.airag.kb.dto.ImportConfirmDTO;
import org.jeecg.modules.airag.kb.dto.ImportTextDTO;
import org.jeecg.modules.airag.kb.vo.ChunkPreviewVO;
import org.jeecg.modules.airag.kb.vo.KnowledgeImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库导入服务。
 */
public interface KnowledgeImportService {
    /**
     * 手动文本导入。
     *
     * @param kbId 知识库ID
     * @param dto 导入请求
     * @return 导入结果
     */
    KnowledgeImportResultVO importText(String kbId, ImportTextDTO dto);

    /**
     * 文件导入。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @param chunkSize 分段长度
     * @param chunkOverlap 分段重叠长度
     * @return 导入结果
     */
    KnowledgeImportResultVO importFile(String kbId, MultipartFile file, Integer chunkSize, Integer chunkOverlap);

    /**
     * 文本切分预览。
     *
     * @param kbId 知识库ID
     * @param dto 导入请求
     * @return chunk预览列表
     */
    List<ChunkPreviewVO> previewText(String kbId, ImportTextDTO dto);

    /**
     * 文件切分预览。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @param chunkSize 分段长度
     * @param chunkOverlap 分段重叠长度
     * @return chunk预览列表
     */
    List<ChunkPreviewVO> previewFile(String kbId, MultipartFile file, Integer chunkSize, Integer chunkOverlap);

    /**
     * 基于预览结果确认导入。
     *
     * @param kbId 知识库ID
     * @param dto 确认请求
     * @return 导入结果
     */
    KnowledgeImportResultVO confirmImport(String kbId, ImportConfirmDTO dto);
}
