package org.jeecg.modules.airag.kb.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.kb.dto.KbQaItemDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * QA导入文件解析器。
 */
final class KbQaImportFileParser {
    /**
     * CSV分隔符。
     */
    private static final char CSV_SEPARATOR = ',';

    /**
     * CSV引号。
     */
    private static final char CSV_QUOTE = '"';

    private KbQaImportFileParser() {
    }

    /**
     * 解析上传文件。
     *
     * @param file 文件
     * @return QA条目列表
     */
    static List<KbQaItemDTO> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".csv")) {
                return parseCsv(file);
            }
            if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
                return parseExcel(file);
            }
        } catch (IOException e) {
            throw new JeecgBootException("文件解析失败：" + e.getMessage());
        }
        throw new JeecgBootException("仅支持CSV、XLS、XLSX文件");
    }

    /**
     * 解析CSV文件。
     *
     * @param file 文件
     * @return QA条目列表
     * @throws IOException IO异常
     */
    private static List<KbQaItemDTO> parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = readNextNonEmptyLine(reader);
            if (oConvertUtils.isEmpty(headerLine)) {
                throw new JeecgBootException("CSV文件为空");
            }
            List<String> headers = parseCsvLine(stripBom(headerLine));
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateHeaders(headerIndex);
            List<KbQaItemDTO> result = new ArrayList<>();
            String line;
            int rowNo = 1;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (oConvertUtils.isEmpty(line) || line.trim().isEmpty()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                KbQaItemDTO item = buildItem(values, headerIndex, rowNo);
                if (item != null) {
                    result.add(item);
                }
            }
            return result;
        }
    }

    /**
     * 解析Excel文件。
     *
     * @param file 文件
     * @return QA条目列表
     * @throws IOException IO异常
     */
    private static List<KbQaItemDTO> parseExcel(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new JeecgBootException("Excel文件为空");
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new JeecgBootException("Excel文件为空");
            }
            DataFormatter formatter = new DataFormatter();
            List<String> headers = new ArrayList<>();
            short lastCellNum = headerRow.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = headerRow.getCell(i);
                headers.add(stripBom(cell == null ? null : formatter.formatCellValue(cell)));
            }
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateHeaders(headerIndex);
            List<KbQaItemDTO> result = new ArrayList<>();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int rowIndex = firstDataRow; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                KbQaItemDTO item = buildItem(readRow(row, formatter), headerIndex, rowIndex + 1);
                if (item != null) {
                    result.add(item);
                }
            }
            return result;
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            throw new JeecgBootException("Excel文件解析失败：" + e.getMessage());
        }
    }

    /**
     * 读取CSV下一行非空文本。
     *
     * @param reader 读取器
     * @return 文本
     * @throws IOException IO异常
     */
    private static String readNextNonEmptyLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                return line;
            }
        }
        return null;
    }

    /**
     * 解析CSV行。
     *
     * @param line 行内容
     * @return 字段列表
     */
    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == CSV_QUOTE) {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == CSV_QUOTE) {
                    current.append(CSV_QUOTE);
                    i++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (ch == CSV_SEPARATOR && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString().trim());
        return values;
    }

    /**
     * 读取Excel行。
     *
     * @param row 行
     * @param formatter 格式化器
     * @return 字段列表
     */
    private static List<String> readRow(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return values;
        }
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            values.add(cell == null ? null : formatter.formatCellValue(cell));
        }
        return values;
    }

    /**
     * 构建表头索引。
     *
     * @param headers 表头列表
     * @return 索引映射
     */
    private static Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = normalizeHeader(headers.get(i));
            if (oConvertUtils.isNotEmpty(header) && !headerIndex.containsKey(header)) {
                headerIndex.put(header, i);
            }
        }
        return headerIndex;
    }

    /**
     * 校验表头。
     *
     * @param headerIndex 表头索引
     */
    private static void validateHeaders(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey("question") || !headerIndex.containsKey("answer")) {
            throw new JeecgBootException("CSV / Excel缺少question或answer列");
        }
    }

    /**
     * 构建条目。
     *
     * @param values 行字段
     * @param headerIndex 表头索引
     * @param rowNo 行号
     * @return 条目
     */
    private static KbQaItemDTO buildItem(List<String> values, Map<String, Integer> headerIndex, int rowNo) {
        String question = getValue(values, headerIndex, "question");
        String answer = getValue(values, headerIndex, "answer");
        String tags = getValue(values, headerIndex, "tags");
        String metadataJson = getValue(values, headerIndex, "metadata_json");
        String sortNoText = getValue(values, headerIndex, "sort_no");
        if (isBlankRow(question, answer, tags, metadataJson, sortNoText)) {
            return null;
        }
        KbQaItemDTO item = new KbQaItemDTO();
        item.setRowNo(rowNo);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setTags(tags);
        item.setMetadataJson(metadataJson);
        item.setSortNo(parseSortNo(sortNoText));
        return item;
    }

    /**
     * 获取字段值。
     *
     * @param values 行字段
     * @param headerIndex 表头索引
     * @param key 字段名
     * @return 字段值
     */
    private static String getValue(List<String> values, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key);
        if (index == null || index < 0 || index >= values.size()) {
            return null;
        }
        String value = values.get(index);
        return oConvertUtils.isEmpty(value) ? null : value.trim();
    }

    /**
     * 解析排序号。
     *
     * @param value 文本
     * @return 排序号
     */
    private static Integer parseSortNo(String value) {
        if (oConvertUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断空行。
     *
     * @param values 行字段
     * @return 是否为空行
     */
    private static boolean isBlankRow(String... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (String value : values) {
            if (oConvertUtils.isNotEmpty(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 去除BOM。
     *
     * @param text 文本
     * @return 文本
     */
    private static String stripBom(String text) {
        if (text != null && text.startsWith("\uFEFF")) {
            return text.substring(1);
        }
        return text;
    }

    /**
     * 标准化表头。
     *
     * @param header 表头
     * @return 标准化结果
     */
    private static String normalizeHeader(String header) {
        if (oConvertUtils.isEmpty(header)) {
            return "";
        }
        String value = header.trim().toLowerCase(Locale.ROOT);
        if ("问题".equals(value)) {
            return "question";
        }
        if ("答案".equals(value)) {
            return "answer";
        }
        if ("标签".equals(value)) {
            return "tags";
        }
        if ("元数据".equals(value) || "元数据json".equals(value)) {
            return "metadata_json";
        }
        if ("排序号".equals(value)) {
            return "sort_no";
        }
        return value;
    }
}
