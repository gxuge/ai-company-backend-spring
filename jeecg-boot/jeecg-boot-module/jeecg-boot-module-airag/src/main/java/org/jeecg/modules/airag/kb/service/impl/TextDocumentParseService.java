package org.jeecg.modules.airag.kb.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.kb.service.DocumentParseService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * TXT文档解析服务。
 */
@Component
public class TextDocumentParseService implements DocumentParseService {
    /**
     * 判断是否支持指定后缀。
     *
     * @param extension 文件后缀
     * @return 是否支持
     */
    @Override
    public boolean supports(String extension) {
        return "txt".equalsIgnoreCase(extension) || "text".equalsIgnoreCase(extension) || "log".equalsIgnoreCase(extension);
    }

    /**
     * 解析TXT内容。
     *
     * @param file 上传文件
     * @return 文本内容
     */
    @Override
    public String parse(MultipartFile file) {
        try {
            return TextFileEncodingUtils.readText(file.getBytes(), "TXT");
        } catch (Exception e) {
            throw new JeecgBootException("TXT文件解析失败：" + e.getMessage());
        }
    }
}
