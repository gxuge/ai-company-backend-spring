package org.jeecg.modules.system.po.tsuserimageasset;

import lombok.Data;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetSaveDto;
import org.jeecg.modules.system.entity.TsUserImageAsset;
@Data
public class TsUserImageAssetSavePo {
    private String fileUrl;
    private String thumbnailUrl;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String sourceType;
    private Integer status;
    public static TsUserImageAssetSavePo fromRequest(TsUserImageAssetSaveDto request) {
        TsUserImageAssetSavePo po = new TsUserImageAssetSavePo();
        if (request == null) {
            return po;
        }
        po.setFileUrl(trimToNull(request.getFileUrl()));
        po.setThumbnailUrl(trimToNull(request.getThumbnailUrl()));
        po.setFileName(trimToNull(request.getFileName()));
        po.setMimeType(trimToNull(request.getMimeType()));
        po.setFileSize(request.getFileSize());
        po.setWidth(request.getWidth());
        po.setHeight(request.getHeight());
        po.setSourceType(trimToNull(request.getSourceType()));
        po.setStatus(request.getStatus());
        return po;
    }
    public void applyTo(TsUserImageAsset entity) {
        if (entity == null) {
            return;
        }
        entity.setFileUrl(this.fileUrl);
        entity.setThumbnailUrl(this.thumbnailUrl);
        entity.setFileName(this.fileName);
        entity.setMimeType(this.mimeType);
        entity.setFileSize(this.fileSize);
        entity.setWidth(this.width);
        entity.setHeight(this.height);
        entity.setSourceType(this.sourceType);
        entity.setStatus(this.status);
    }
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
