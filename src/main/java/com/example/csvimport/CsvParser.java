package com.example.csvimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CSVの読み込みと、値の変換を行う。 */
public final class CsvParser {

    private CsvParser() {
    }

    /** 日付の入力形式('2026/11/13')。存在しない日付(2026/02/30 など)は不正として扱う。 */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT);

    /** 1行分のデータ。lineNumberはヘッダーを1行目とした物理行番号。 */
    public record Row(int lineNumber, Map<String, String> values) {
    }

    /**
     * CSVを読み、データ行を返す。空行は読み飛ばす。
     * 列数が足りない行は、足りない列がMapに含まれない(get が null を返す)。
     * ダブルクォートで囲んだ項目(カンマを含む項目)に対応するが、項目内の改行には対応しない。
     */
    public static List<Row> readRows(Path path, Charset charset) throws IOException {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            List<String> header = splitLine(headerLine);
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) {
                    continue;
                }
                List<String> fields = splitLine(line);
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < header.size() && i < fields.size(); i++) {
                    values.put(header.get(i), fields.get(i));
                }
                rows.add(new Row(lineNumber, values));
            }
        }
        return rows;
    }

    /** 1行をカンマで分割する。ダブルクォート内のカンマと、"" によるエスケープを扱う。 */
    static List<String> splitLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /** '2026/11/13' 形式の文字列を LocalDate に変換する。不正な値は DateTimeParseException。 */
    public static LocalDate parseDate(String text) {
        return LocalDate.parse(text, DATE_FORMAT);
    }
}
