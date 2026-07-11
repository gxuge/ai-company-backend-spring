package org.jeecg.modules.airag.kb.service;

import org.jeecg.modules.airag.kb.dto.KbFederatedSearchQueryDTO;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;

/**
 * 多知识库联邦检索服务。
 */
public interface IKbFederatedSearchService {
    /**
     * 执行联邦检索。
     *
     * @param dto 请求
     * @return 结果
     */
    KbSemanticSearchResultVO search(KbFederatedSearchQueryDTO dto);
}
