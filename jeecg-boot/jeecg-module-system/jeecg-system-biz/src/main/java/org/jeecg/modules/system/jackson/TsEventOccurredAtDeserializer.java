package org.jeecg.modules.system.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

/** 兼容解析客户端事件发生时间。 */
public class TsEventOccurredAtDeserializer extends JsonDeserializer<Date> {
    private static final DateTimeFormatter LEGACY_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT);
    private static final ZoneOffset LEGACY_ZONE_OFFSET = ZoneOffset.ofHours(8);

    /** 支持 ISO 8601、旧版 GMT+8 时间文本和毫秒时间戳。 */
    @Override
    public Date deserialize(
            JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return new Date(parser.getLongValue());
        }
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            context.reportInputMismatch(
                    Date.class,
                    "事件时间必须是 ISO 8601、yyyy-MM-dd HH:mm:ss 或毫秒时间戳");
            return null;
        }
        String value = parser.getText().trim();
        if (value.isEmpty()) {
            return null;
        }
        Date parsed = parseIsoDate(value);
        if (parsed != null) {
            return parsed;
        }
        try {
            LocalDateTime localDateTime =
                    LocalDateTime.parse(value, LEGACY_FORMATTER);
            return Date.from(localDateTime.toInstant(LEGACY_ZONE_OFFSET));
        } catch (DateTimeParseException exception) {
            throw context.weirdStringException(
                    value,
                    Date.class,
                    "事件时间仅支持 ISO 8601 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    /** 解析包含 UTC 标记或时区偏移的 ISO 8601 时间。 */
    private Date parseIsoDate(String value) {
        try {
            return Date.from(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            // Continue with ISO offset parsing.
        }
        try {
            return Date.from(OffsetDateTime.parse(
                    value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
