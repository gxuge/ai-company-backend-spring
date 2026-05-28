package org.jeecg.config;

import com.yomahub.liteflow.core.FlowExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "liteflow.enable", havingValue = "false")
public class LiteflowDisabledFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(FlowExecutor.class)
    public FlowExecutor flowExecutor() {
        return new FlowExecutor();
    }
}

