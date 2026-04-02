package org.jeecg.modules.openapi.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.IpUtils;
import org.jeecg.modules.openapi.config.MiniMaxDemoGuardConfigBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * MiniMax demo guard service.
 */
@Service
public class MiniMaxDemoGuardService {

    private final MiniMaxDemoGuardConfigBean guardConfig;
    private final Map<String, Deque<Long>> rateLimitStore = new ConcurrentHashMap<>();

    public MiniMaxDemoGuardService(MiniMaxDemoGuardConfigBean guardConfig) {
        this.guardConfig = guardConfig;
    }

    /**
     * Validate token and apply per-IP rate limit.
     */
    public void checkRequest(HttpServletRequest request, String apiName) {
        if (!guardConfig.isEnabled()) {
            return;
        }
        String accessToken = request.getHeader(CommonConstant.X_ACCESS_TOKEN);
        boolean hasLoginToken = StringUtils.hasText(accessToken);
        if (!hasLoginToken && StringUtils.hasText(guardConfig.getAccessToken())) {
            String requestToken = request.getHeader(guardConfig.getHeaderName());
            if (!guardConfig.getAccessToken().equals(requestToken)) {
                throw new JeecgBootBizTipException("Unauthorized request");
            }
        }
        String ip = IpUtils.getIpAddr(request);
        String limitKey = apiName + ":" + ip;
        if (!allowRequest(limitKey)) {
            throw new JeecgBootBizTipException("Request rate is too high, please retry later");
        }
    }

    /**
     * Sliding window limiter in one-minute window.
     */
    private boolean allowRequest(String key) {
        Deque<Long> window = rateLimitStore.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        long now = System.currentTimeMillis();
        long expireBefore = now - 60_000L;
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() < expireBefore) {
                window.pollFirst();
            }
            if (window.size() >= guardConfig.getMaxRequestsPerMinute()) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }
}
