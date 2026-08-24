package org.jeecg.modules.system.vo.tsworkreview;

import lombok.Data;
import org.jeecg.modules.system.entity.TsWorkReviewItem;
import org.jeecg.modules.system.entity.TsWorkReviewLog;

import java.util.Date;
import java.util.List;

@Data
public class TsWorkReviewVo {
    private Long id;
    private String reviewNo;
    private String workType;
    private Long workId;
    private String workTitle;
    private String ownerUserId;
    private Integer workVersion;
    private Integer requestedPublic;
    private String snapshotJson;
    private String snapshotHash;
    private String status;
    private String aiDecision;
    private String aiRiskLevel;
    private String aiReason;
    private String aiResultJson;
    private Date aiReviewedAt;
    private String adminReviewerId;
    private String adminReason;
    private Date adminReviewedAt;
    private Date submittedAt;
    private List<TsWorkReviewItem> items;
    private List<TsWorkReviewLog> logs;
}
