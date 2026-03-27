package org.jeecg.modules.system.dto.tschatsession;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
@Data
public class TsChatSessionQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String sessionType;
    private Integer sessionStatus;
    private Long targetRoleId;
    private Long storyId;
    private String keyword;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageAtStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageAtEnd;
}
