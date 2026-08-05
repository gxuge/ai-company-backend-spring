package org.jeecg.modules.system.vo.tsmember;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * PRO 与 ULTRA 权益对比。
 */
@Data
public class TsMemberCompareVo {

    /** PRO 权益。 */
    private List<TsMemberPageVo.BenefitVo> proBenefits = new ArrayList<>();
    /** ULTRA 权益。 */
    private List<TsMemberPageVo.BenefitVo> ultraBenefits = new ArrayList<>();
    /** 权益差异。 */
    private List<DifferenceVo> differences = new ArrayList<>();

    /**
     * 单项权益差异。
     */
    @Data
    public static class DifferenceVo {
        /** 权益编码。 */
        private String benefitCode;
        /** 权益名称。 */
        private String benefitName;
        /** PRO 权益展示值。 */
        private String proValue;
        /** ULTRA 权益展示值。 */
        private String ultraValue;
        /** 两个等级是否存在差异。 */
        private Boolean different;
    }
}
