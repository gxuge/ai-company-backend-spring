package org.jeecg.modules.airag.kb.service.impl;

import org.apache.tika.Tika;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.kb.service.DocumentParseService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 基于Tika的文档解析服务，作为DOCX/PDF等格式的兜底解析。
 */
@Component
public class TikaDocumentParseService implements DocumentParseService {
    /**
     * Tika解析器。
     */
    private final Tika tika = new Tika();

    /**
     * 判断是否支持指定后缀。
     *
     * @param extension 文件后缀
     * @return 是否支持
     */
    @Override
    public boolean supports(String extension) {
        return "docx".equalsIgnoreCase(extension) || "pdf".equalsIgnoreCase(extension) || "doc".equalsIgnoreCase(extension);
    }

    /**
     * 解析文件内容。
     *
     * @param file 上传文件
     * @return 文本内容
     */
    @Override
    public String parse(MultipartFile file) {
        try {
            String text = tika.parseToString(file.getInputStream());
            if (text == null) {
                return "";
            }
            return text;
        } catch (Exception e) {
            throw new JeecgBootException("文档解析失败：" + e.getMessage());
        }
    }
}
