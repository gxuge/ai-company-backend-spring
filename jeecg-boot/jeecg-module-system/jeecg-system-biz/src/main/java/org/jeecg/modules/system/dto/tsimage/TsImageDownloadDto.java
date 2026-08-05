package org.jeecg.modules.system.dto.tsimage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TS 远程图片下载请求。
 */
@Data
public class TsImageDownloadDto {

    /**
     * 待下载的公网图片地址。
     */
    @NotBlank(message = "图片URL不能为空")
    @Size(max = 8192, message = "图片URL长度不能超过8192个字符")
    private String sourceImageUrl;

    /**
     * 下载文件名，可不包含扩展名。
     */
    @Size(max = 180, message = "文件名长度不能超过180个字符")
    private String fileName;
}
