package org.jeecg.modules.system.vo.tschatsession;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One-time ASR WebSocket connection ticket.
 */
@Data
@AllArgsConstructor
public class TsChatAsrTicketVo {

    private String ticket;
    private long expiresInSeconds;
}
