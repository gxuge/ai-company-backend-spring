package org.jeecg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ai配置类，通用的配置可以放到这里面
 *
 * @Author: wangshuai
 * @Date: 2025/12/17 14:00
 */
@Data
@Component
@ConfigurationProperties(prefix = AiRagConfigBean.PREFIX)
public class AiRagConfigBean {
    public static final String PREFIX = "jeecg.airag";
    
    /**
     * 敏感节点
     * stdio mpc命令行功能开启，sql：AI流程SQL节点开启
     */
    private String allowSensitiveNodes = "";

    /**
     * MCP Server 配置。
     */
    private McpServer mcpServer = new McpServer();

    @Data
    public static class McpServer {
        /**
         * 是否启用内置 MCP Server。
         */
        private boolean enabled = true;

        /**
         * 可选的 API Key。为空时不校验。
         */
        private String apiKey = "";

        /**
         * 公开角色列表默认条数。
         */
        private int defaultRoleListLimit = 10;

        /**
         * 公开角色列表最大条数。
         */
        private int maxRoleListLimit = 50;
    }
}
