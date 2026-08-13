package org.jeecg.modules.system.vo.tsfeedback;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 反馈详情展示对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TsFeedbackDetailVo extends TsFeedbackListVo {

    /** 追加反馈列表，按创建时间升序。 */
    private List<TsFeedbackAppendVo> appends;

    /** 反馈附件列表。 */
    private List<TsFeedbackAttachmentVo> attachments;
}
