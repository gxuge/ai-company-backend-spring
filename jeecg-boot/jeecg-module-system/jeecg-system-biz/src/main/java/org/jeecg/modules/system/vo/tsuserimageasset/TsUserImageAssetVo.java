package org.jeecg.modules.system.vo.tsuserimageasset;

import lombok.Data;

import java.util.Date;
@Data
public class TsUserImageAssetVo {
    private Long id;
    private Long userId;
    private String fileUrl;
    private String thumbnailUrl;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String sourceType;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
