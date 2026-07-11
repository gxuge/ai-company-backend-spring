package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.kb.dto.KbDocumentCreateDto;
import org.jeecg.modules.airag.kb.dto.KbDocumentQueryDto;
import org.jeecg.modules.airag.kb.entity.KbDocument;
import org.jeecg.modules.airag.kb.vo.KbDocumentVo;

/**
 * 知识库文档服务。
 */
public interface IKbDocumentService extends IService<KbDocument> {
    /**
     * 创建文档记录。
     *
     * @param kbId 知识库ID
     * @param dto 创建请求
     * @return 文档返回对象
     */
    KbDocumentVo createDocument(String kbId, KbDocumentCreateDto dto);

    /**
     * 分页查询文档。
     *
     * @param kbId 知识库ID
     * @param query 查询请求
     * @return 分页结果
     */
    IPage<KbDocumentVo> listDocuments(String kbId, KbDocumentQueryDto query);

    /**
     * 删除文档并级联禁用chunk。
     *
     * @param documentId 文档ID
     */
    void deleteDocument(String documentId);
}
