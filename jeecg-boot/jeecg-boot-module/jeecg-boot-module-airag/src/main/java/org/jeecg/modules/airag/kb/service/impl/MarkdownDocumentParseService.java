package org.jeecg.modules.airag.kb.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.kb.service.DocumentParseService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Markdown文档解析服务。
 */
@Component
public class MarkdownDocumentParseService implements DocumentParseService {
    /**
     * 判断是否支持指定后缀。
     *
     * @param extension 文件后缀
     * @return 是否支持
     */
    @Override
    public boolean supports(String extension) {
        return "md".equalsIgnoreCase(extension) || "markdown".equalsIgnoreCase(extension);
    }

    /**
     * 解析Markdown内容。
     *
     * @param file 上传文件
     * @return 文本内容
     */
    @Override
    public String parse(MultipartFile file) {
        try {
            return TextFileEncodingUtils.readText(file.getBytes(), "MD");
        } catch (Exception e) {
            throw new JeecgBootException("Markdown文件解析失败：" + e.getMessage());
        }
    }
}
