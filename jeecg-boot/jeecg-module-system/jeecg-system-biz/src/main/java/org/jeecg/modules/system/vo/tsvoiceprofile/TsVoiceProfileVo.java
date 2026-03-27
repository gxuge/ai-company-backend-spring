package org.jeecg.modules.system.vo.tsvoiceprofile;

import lombok.Data;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;

import java.util.Date;
import java.util.List;
@Data
public class TsVoiceProfileVo {
    private Long id;
    private String name;
    private String avatarUrl;
    private String gender;
    private String ageGroup;
    private Integer status;
    private Integer sortNo;
    private List<Long> tagIds;
    private List<TsVoiceTagVo> tags;
    private Date createdAt;
    private Date updatedAt;
}
