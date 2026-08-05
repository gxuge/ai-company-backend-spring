package org.jeecg.modules.system.vo.tsmember;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 会员首页配置。
 */
@Data
public class TsMemberPageVo {

    /** 会员等级列表。 */
    private List<PlanVo> plans = new ArrayList<>();
    /** 推荐套餐 ID 列表。 */
    private List<Long> recommendedProductIds = new ArrayList<>();

    /**
     * 会员等级展示项。
     */
    @Data
    public static class PlanVo {
        /** 会员等级 ID。 */
        private Long id;
        /** 会员名称。 */
        private String name;
        /** 会员编码。 */
        private String code;
        /** 会员说明。 */
        private String description;
        /** 主题颜色。 */
        private String themeColor;
        /** 套餐列表。 */
        private List<ProductVo> products = new ArrayList<>();
        /** 权益列表。 */
        private List<BenefitVo> benefits = new ArrayList<>();
        /** 开通赠礼。 */
        private List<GiftVo> gifts = new ArrayList<>();
    }

    /**
     * 会员套餐展示项。
     */
    @Data
    public static class ProductVo {
        /** 套餐 ID。 */
        private Long id;
        /** 周期：WEEK、MONTH、QUARTER、YEAR。 */
        private String cycleType;
        /** 售价。 */
        private BigDecimal price;
        /** 原价。 */
        private BigDecimal originalPrice;
        /** 优惠说明。 */
        private String discountText;
        /** 是否推荐。 */
        private Boolean recommended;
    }

    /**
     * 会员权益展示项。
     */
    @Data
    public static class BenefitVo {
        /** 权益编码。 */
        private String code;
        /** 权益名称。 */
        private String name;
        /** 权益说明。 */
        private String description;
        /** 图标。 */
        private String icon;
        /** 分类。 */
        private String category;
        /** 权益值。 */
        private String value;
        /** 单位。 */
        private String unit;
        /** 限制类型。 */
        private String limitType;
        /** 用于前端直接展示的权益文本。 */
        private String displayValue;
    }

    /**
     * 开通赠礼展示项。
     */
    @Data
    public static class GiftVo {
        /** 赠礼 ID。 */
        private Long id;
        /** 赠礼名称。 */
        private String name;
        /** 赠礼说明。 */
        private String description;
        /** 图标。 */
        private String icon;
    }
}
