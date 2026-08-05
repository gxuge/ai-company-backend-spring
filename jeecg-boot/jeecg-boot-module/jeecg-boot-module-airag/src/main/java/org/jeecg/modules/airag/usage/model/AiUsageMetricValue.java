package org.jeecg.modules.airag.usage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One normalized usage metric.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageMetricValue {

    private String code;
    private BigDecimal value;
    private String unit;
    private String scope;
    private String extJson;

    public static AiUsageMetricValue of(String code, Number value, String unit, String scope) {
        if (value == null) {
            return null;
        }
        return new AiUsageMetricValue(
                code,
                new BigDecimal(String.valueOf(value)),
                unit,
                scope,
                null
        );
    }
}
