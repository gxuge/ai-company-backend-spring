package org.jeecg.modules.system.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.dto.tsad.TsAdEventReportDto;
import org.jeecg.modules.system.dto.tsbehavior.TsBehaviorBatchDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 事件发生时间兼容反序列化测试。 */
class TsEventOccurredAtDeserializerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 行为批量上报应接受浏览器生成的 UTC ISO 时间。 */
    @Test
    void shouldDeserializeIsoInstantForBehaviorBatch() throws Exception {
        String json = """
                {
                  "events": [{
                    "eventId": "event-1",
                    "eventType": "view",
                    "sessionId": "session-1",
                    "occurredAt": "2026-08-26T12:15:39.125Z"
                  }]
                }
                """;

        TsBehaviorBatchDto batch =
                objectMapper.readValue(json, TsBehaviorBatchDto.class);

        assertEquals(
                Instant.parse("2026-08-26T12:15:39.125Z").toEpochMilli(),
                batch.getEvents().get(0).getOccurredAt().getTime());
    }

    /** 广告事件应接受携带 GMT+8 偏移的 ISO 时间。 */
    @Test
    void shouldDeserializeIsoOffsetForAdEvent() throws Exception {
        String json = """
                {
                  "eventId": "event-2",
                  "contentId": 1,
                  "slotCode": "HOME_BANNER",
                  "eventType": "IMPRESSION",
                  "platform": "WEB",
                  "occurredAt": "2026-08-26T20:15:39.125+08:00"
                }
                """;

        TsAdEventReportDto event =
                objectMapper.readValue(json, TsAdEventReportDto.class);

        assertEquals(
                Instant.parse("2026-08-26T12:15:39.125Z").toEpochMilli(),
                event.getOccurredAt().getTime());
    }

    /** 原有无时区格式应继续按 GMT+8 解析。 */
    @Test
    void shouldDeserializeLegacyDateTimeAsGmtEight() throws Exception {
        String json = """
                {
                  "eventId": "event-3",
                  "contentId": 1,
                  "slotCode": "HOME_BANNER",
                  "eventType": "CLICK",
                  "platform": "WEB",
                  "occurredAt": "2026-08-26 20:15:39"
                }
                """;

        TsAdEventReportDto event =
                objectMapper.readValue(json, TsAdEventReportDto.class);

        assertEquals(
                Instant.parse("2026-08-26T12:15:39Z").toEpochMilli(),
                event.getOccurredAt().getTime());
    }

    /** 非法时间应返回明确的 JSON 映射异常。 */
    @Test
    void shouldRejectUnsupportedDateFormat() {
        String json = """
                {
                  "eventId": "event-4",
                  "contentId": 1,
                  "slotCode": "HOME_BANNER",
                  "eventType": "CLICK",
                  "platform": "WEB",
                  "occurredAt": "2026/08/26 20:15:39"
                }
                """;

        JsonMappingException exception = assertThrows(
                JsonMappingException.class,
                () -> objectMapper.readValue(json, TsAdEventReportDto.class));

        assertTrue(exception.getMessage().contains("事件时间仅支持"));
    }
}
