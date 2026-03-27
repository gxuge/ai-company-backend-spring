package org.jeecg.modules.system.vo.tsvoicetag;

import lombok.Data;

import java.util.Date;
@Data
public class TsVoiceTagVo {
    private Long id;
    private String tagName;
    private Date createdAt;
}
