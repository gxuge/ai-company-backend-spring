package org.jeecg.modules.airag.prompts.service;

import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.prompts.entity.AiragPrompts;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.prompts.vo.AiragExperimentVo;

/**
 * @Description: airag_prompts
 * @Author: jeecg-boot
 * @Date:   2025-12-12
 * @Version: V1.0
 */
public interface IAiragPromptsService extends IService<AiragPrompts> {

    Result<?> promptExperiment(AiragExperimentVo experimentVo, HttpServletRequest request);

    /**
     * 将 classpath 模板批量同步到 airag_prompts（存在则更新，不存在则新增）。
     *
     * @return 同步数量
     */
    int syncClasspathTemplatesToDb();
}
