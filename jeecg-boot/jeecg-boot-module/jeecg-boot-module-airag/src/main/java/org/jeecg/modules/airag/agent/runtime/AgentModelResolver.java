package org.jeecg.modules.airag.agent.runtime;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.springframework.stereotype.Component;

/**
 * 按 appId 解析文本模型。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
@RequiredArgsConstructor
public class AgentModelResolver {
    /**
     * 应用 Mapper。
     */
    private final AiragAppMapper airagAppMapper;

    /**
     * 解析指定应用绑定的文本模型ID。
     *
     * @param appId 应用ID
     * @return 文本模型ID
     */
    public String resolveTextModelId(String appId) {
        if (oConvertUtils.isEmpty(appId)) {
            throw new AgentErrorException(AgentErrorCode.LLM_MODEL_APP_ID_REQUIRED);
        }
        AiragApp airagApp = this.airagAppMapper.getByIdIgnoreTenant(appId);
        if (airagApp == null) {
            throw new AgentErrorException(
                    AgentErrorCode.LLM_MODEL_APP_NOT_FOUND,
                    java.util.Map.of("appId", appId)
            );
        }
        if (oConvertUtils.isEmpty(airagApp.getModelId())) {
            throw new AgentErrorException(
                    AgentErrorCode.LLM_MODEL_NOT_CONFIGURED,
                    java.util.Map.of("appId", appId)
            );
        }
        return airagApp.getModelId();
    }
}
