package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.airag.kb.dto.KbRetrievalTestLogQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbSemanticSearchQueryDTO;
import org.jeecg.modules.airag.kb.vo.KbRetrievalTestLogVo;
import org.jeecg.modules.airag.kb.vo.KbSemanticSearchResultVO;

/**
 * 检索测试服务。
 */
public interface IKbRetrievalTestService {
    /**
     * 执行检索测试。
     *
     * @param kbId 知识库ID
     * @param dto 请求
     * @return 检索结果
     */
    KbSemanticSearchResultVO testSearch(String kbId, KbSemanticSearchQueryDTO dto);

    /**
     * 分页查询日志。
     *
     * @param dto 查询条件
     * @return 日志分页
     */
    IPage<KbRetrievalTestLogVo> pageLogs(KbRetrievalTestLogQueryDTO dto);

    /**
     * 查询日志详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    KbRetrievalTestLogVo getLogById(String id);
}
