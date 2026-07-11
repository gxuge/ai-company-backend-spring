package org.jeecg.modules.airag.kb.service.impl;

import org.jeecg.common.exception.JeecgBootException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * 文本文件编码识别工具。
 */
final class TextFileEncodingUtils {
    /**
     * 候选编码。
     */
    private static final Charset[] CANDIDATES = new Charset[] {
            StandardCharsets.UTF_8,
            Charset.forName("GB18030"),
            StandardCharsets.UTF_16LE,
            StandardCharsets.UTF_16BE,
            StandardCharsets.UTF_16
    };

    private TextFileEncodingUtils() {
    }

    /**
     * 按尽可能合理的编码读取文本。
     *
     * @param bytes 文件字节
     * @param fileType 文件类型
     * @return 文本内容
     */
    static String readText(byte[] bytes, String fileType) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        Charset bomCharset = detectBomCharset(bytes);
        if (bomCharset != null) {
            return decodeStrict(bytes, bomCharset);
        }
        String bestText = null;
        int bestScore = Integer.MIN_VALUE;
        for (Charset charset : CANDIDATES) {
            try {
                String text = decodeStrict(bytes, charset);
                int score = score(text);
                if (score > bestScore) {
                    bestScore = score;
                    bestText = text;
                }
            } catch (Exception ignored) {
                // 继续尝试下一个候选编码。
            }
        }
        if (bestText == null) {
            throw new JeecgBootException(fileType + "文件解析失败：无法识别文件编码");
        }
        return bestText;
    }

    /**
     * 识别BOM编码。
     *
     * @param bytes 文件字节
     * @return 编码
     */
    private static Charset detectBomCharset(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2) {
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if ((bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
        }
        return null;
    }

    /**
     * 严格解码。
     *
     * @param bytes 文件字节
     * @param charset 编码
     * @return 文本内容
     */
    private static String decodeStrict(byte[] bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(bytes));
            String text = charBuffer.toString();
            if (text.startsWith("\uFEFF")) {
                return text.substring(1);
            }
            return text;
        } catch (Exception e) {
            throw new IllegalArgumentException("decode failed", e);
        }
    }

    /**
     * 简单评分，越像正常文本分越高。
     *
     * @param text 文本内容
     * @return 评分
     */
    private static int score(String text) {
        if (text == null || text.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t') {
                score -= 3;
            } else if (Character.isLetterOrDigit(ch) || Character.isWhitespace(ch)) {
                score += 2;
            } else {
                score += 1;
            }
        }
        return score;
    }
}
