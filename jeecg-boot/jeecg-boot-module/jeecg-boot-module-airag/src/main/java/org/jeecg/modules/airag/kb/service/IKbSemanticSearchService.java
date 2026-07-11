package org.jeecg.modules.airag.kb.service;

import org.jeecg.modules.airag.kb.dto.KbSemanticSearchQueryDTO;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;

import java.util.Map;

/**
 * 知识库语义检索服务。
 */
public interface IKbSemanticSearchService {
    /**
     * 执行语义检索。
     *
     * @param kbId 知识库ID
     * @param dto 检索请求
     * @return 检索结果
     */
    KbSemanticSearchResultVO search(String kbId, KbSemanticSearchQueryDTO dto);

    /**
     * 取出最近一次检索快照并清理。
     *
     * @return 检索快照
     */
    Map<String, Object> consumeLastSearchTrace();
}
