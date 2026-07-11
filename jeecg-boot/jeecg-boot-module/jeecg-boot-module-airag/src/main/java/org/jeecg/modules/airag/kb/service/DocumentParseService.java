package org.jeecg.modules.airag.kb.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析服务。
 */
public interface DocumentParseService {
    /**
     * 判断是否支持指定文件后缀。
     *
     * @param extension 文件后缀
     * @return 是否支持
     */
    boolean supports(String extension);

    /**
     * 解析文件内容。
     *
     * @param file 上传文件
     * @return 解析后的文本内容
     */
    String parse(MultipartFile file);
}
