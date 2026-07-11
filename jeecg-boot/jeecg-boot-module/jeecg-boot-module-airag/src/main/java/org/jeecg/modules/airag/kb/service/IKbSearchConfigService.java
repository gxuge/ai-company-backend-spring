package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.kb.dto.KbSearchConfigSaveDto;
import org.jeecg.modules.airag.kb.entity.KbSearchConfig;
import org.jeecg.modules.airag.kb.vo.KbSearchConfigVo;

/**
 * 知识库检索配置服务。
 */
public interface IKbSearchConfigService extends IService<KbSearchConfig> {
    /**
     * 创建或更新知识库检索配置。
     *
     * @param kbId 知识库ID
     * @param dto 配置请求
     * @return 配置返回对象
     */
    KbSearchConfigVo saveOrUpdateConfig(String kbId, KbSearchConfigSaveDto dto);

    /**
     * 查询知识库检索配置，不存在时自动创建默认配置。
     *
     * @param kbId 知识库ID
     * @return 配置返回对象
     */
    KbSearchConfigVo getByKbId(String kbId);
}
