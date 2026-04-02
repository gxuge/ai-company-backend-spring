package org.jeecg.modules.airag.prompts.service;

import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;

import java.util.Map;

/**
 * Classpath 提示词模板服务
 *
 * @author chenrui
 * @date 2026/3/31
 */
public interface IAiragPromptTemplateService {

    /**
     * 按模板编码与版本查询模板
     *
     * @param code 模板编码
     * @param version 模板版本
     * @return 模板内容
     */
    AiragPromptTemplateVo getTemplate(String code, String version);

    /**
     * 渲染指定 section（仅替换 {{key}} 占位符）
     *
     * @param code 模板编码
     * @param version 模板版本
     * @param sectionName section 名称
     * @param variables 变量
     * @return 渲染后的文本
     */
    String renderSection(String code, String version, String sectionName, Map<String, String> variables);
}
