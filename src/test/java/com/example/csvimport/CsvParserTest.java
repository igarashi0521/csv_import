package com.example.csvimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CsvParserTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path writeCsv(String content) throws IOException {
        Path path = tmp.newFile("a.csv").toPath();
        Files.writeString(path, content, Config.CSV_CHARSET);
        return path;
    }

    @Test
    public void スラッシュ区切りをLocalDateに変換できる() {
        assertEquals(LocalDate.of(2026, 11, 2), CsvParser.parseDate("2026/11/02"));
    }

    @Test
    public void 不正な日付はDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> CsvParser.parseDate("2026-11-02"));
        assertThrows(DateTimeParseException.class, () -> CsvParser.parseDate("2026/02/30"));
    }

    @Test
    public void 行番号はヘッダーを1行目として数える() throws IOException {
        Path path = writeCsv("a,b\r\n1,2\r\n3,4\r\n");
        List<CsvParser.Row> rows = CsvParser.readRows(path, Config.CSV_CHARSET);
        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).lineNumber());
        assertEquals(3, rows.get(1).lineNumber());
        assertEquals("1", rows.get(0).values().get("a"));
        assertEquals("2", rows.get(0).values().get("b"));
    }

    @Test
    public void 空行は読み飛ばすが行番号は進む() throws IOException {
        Path path = writeCsv("a,b\r\n1,2\r\n\r\n3,4\r\n");
        List<CsvParser.Row> rows = CsvParser.readRows(path, Config.CSV_CHARSET);
        assertEquals(List.of(2, 4), List.of(rows.get(0).lineNumber(), rows.get(1).lineNumber()));
    }

    @Test
    public void 空ファイルとヘッダーのみは0件() throws IOException {
        assertEquals(0, CsvParser.readRows(writeCsv(""), Config.CSV_CHARSET).size());
        Path headerOnly = tmp.newFile("b.csv").toPath();
        Files.writeString(headerOnly, "a,b\r\n", Config.CSV_CHARSET);
        assertEquals(0, CsvParser.readRows(headerOnly, Config.CSV_CHARSET).size());
    }

    @Test
    public void 列が足りない行は足りない列がnull() throws IOException {
        Path path = writeCsv("a,b,c\r\n1,2\r\n");
        CsvParser.Row row = CsvParser.readRows(path, Config.CSV_CHARSET).get(0);
        assertEquals("2", row.values().get("b"));
        assertNull(row.values().get("c"));
    }

    @Test
    public void ダブルクォート内のカンマとエスケープを扱える() {
        assertEquals(List.of("a", "b,c", "d\"e", ""), CsvParser.splitLine("a,\"b,c\",\"d\"\"e\","));
    }
}
