package org.jeecg.modules.system.service;

import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.modules.system.dto.tsimage.TsImageDownloadDto;

/**
 * TS 通用图片服务。
 */
public interface ITsImageService {

    /**
     * 校验并代理下载远程图片，不执行图片入库。
     *
     * @param request 下载请求
     * @param response HTTP 响应
     */
    void downloadImage(TsImageDownloadDto request, HttpServletResponse response);
}
