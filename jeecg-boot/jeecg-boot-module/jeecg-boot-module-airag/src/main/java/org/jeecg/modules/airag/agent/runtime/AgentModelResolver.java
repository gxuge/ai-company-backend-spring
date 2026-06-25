package org.jeecg.modules.airag.agent.runtime;

import lombok.RequiredArgsConstructor;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.AssertUtils;
import org.jeecg.common.util.oConvertUtils;
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
        AssertUtils.assertNotEmpty("appId不能为空", appId);
        AiragApp airagApp = this.airagAppMapper.getByIdIgnoreTenant(appId);
        if (airagApp == null) {
            throw new JeecgBootBizTipException("未找到应用配置：" + appId);
        }
        if (oConvertUtils.isEmpty(airagApp.getModelId())) {
            throw new JeecgBootBizTipException("应用未绑定文本模型：" + appId);
        }
        return airagApp.getModelId();
    }
}
