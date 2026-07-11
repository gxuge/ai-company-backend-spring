package org.jeecg.modules.airag.kb.service;

import org.jeecg.modules.airag.kb.dto.KbAutoQuestionDTO;
import org.jeecg.modules.airag.kb.dto.KbChunkIndexSaveDto;
import org.jeecg.modules.airag.kb.dto.KbQaBatchDTO;
import org.jeecg.modules.airag.kb.dto.KbQaImportConfirmDTO;
import org.jeecg.modules.airag.kb.dto.KbQaItemDTO;
import org.jeecg.modules.airag.kb.vo.KbAutoQuestionPreviewVO;
import org.jeecg.modules.airag.kb.vo.KbChunkIndexVo;
import org.jeecg.modules.airag.kb.vo.KbQaImportPreviewVO;
import org.jeecg.modules.airag.kb.vo.KbQaImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * QA导入与多索引服务。
 */
public interface IKbQaService {
    /**
     * 手动新增单条QA。
     *
     * @param kbId 知识库ID
     * @param dto QA条目
     * @return 导入结果
     */
    KbQaImportResultVO createQa(String kbId, KbQaItemDTO dto);

    /**
     * 批量新增QA。
     *
     * @param kbId 知识库ID
     * @param dto 批量请求
     * @return 导入结果
     */
    KbQaImportResultVO batchCreateQa(String kbId, KbQaBatchDTO dto);

    /**
     * CSV/Excel导入预览。
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @return 预览结果
     */
    KbQaImportPreviewVO previewImport(String kbId, MultipartFile file);

    /**
     * CSV/Excel确认导入。
     *
     * @param kbId 知识库ID
     * @param dto 确认请求
     * @return 导入结果
     */
    KbQaImportResultVO confirmImport(String kbId, KbQaImportConfirmDTO dto);

    /**
     * 预览自动生成的索引问题（基于chunk）。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dto 请求
     * @return 预览结果
     */
    KbAutoQuestionPreviewVO previewAutoQuestions(String kbId, String chunkId, KbAutoQuestionDTO dto);

    /**
     * 预览自动生成的索引问题（基于文本）。
     *
     * @param kbId 知识库ID
     * @param dto 请求
     * @return 预览结果
     */
    KbAutoQuestionPreviewVO previewAutoQuestions(String kbId, KbAutoQuestionDTO dto);

    /**
     * 保存自动生成的索引问题。
     *
     * @param kbId 知识库ID
     * @param chunkId chunk ID
     * @param dto 请求
     * @return 创建的索引列表
     */
    List<KbChunkIndexVo> saveAutoQuestions(String kbId, String chunkId, KbAutoQuestionDTO dto);
}
