package org.jeecg.modules.system.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.system.dto.tsimage.TsImageDownloadDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TsImageServiceImplTest {

    @Test
    void shouldRejectLoopbackImageUrl() {
        TsImageDownloadDto request = new TsImageDownloadDto();
        request.setSourceImageUrl("http://127.0.0.1/internal.png");
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        JeecgBootBizTipException error = Assertions.assertThrows(
                JeecgBootBizTipException.class,
                () -> new TsImageServiceImpl().downloadImage(request, response)
        );

        Assertions.assertEquals("不允许访问内网图片地址", error.getMessage());
        Mockito.verify(response).reset();
    }

    @Test
    void shouldRejectNonHttpImageUrl() {
        TsImageDownloadDto request = new TsImageDownloadDto();
        request.setSourceImageUrl("file:///tmp/image.png");
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        JeecgBootBizTipException error = Assertions.assertThrows(
                JeecgBootBizTipException.class,
                () -> new TsImageServiceImpl().downloadImage(request, response)
        );

        Assertions.assertEquals("仅支持HTTP或HTTPS图片地址", error.getMessage());
        Mockito.verify(response).reset();
    }
}
