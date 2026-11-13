package com.example.csvimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BatchTest {

    private static final String HEADER =
            "order_id,customer_code,order_date,product_code,quantity,unit_price\r\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String db;
    private Path log;

    @Before
    public void setUp() {
        db = tmp.getRoot().toPath().resolve("test.db").toString();
        log = tmp.getRoot().toPath().resolve("error.log");
    }

    private Path writeCsv(String body) throws IOException {
        Path path = tmp.getRoot().toPath().resolve("in.csv");
        Files.writeString(path, HEADER + body, Config.CSV_CHARSET);
        return path;
    }

    private int count() throws SQLException {
        try (Connection conn = Repository.connect(db);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM orders")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    public void 正常なCSVは全件登録される() throws Exception {
        Path csv = writeCsv("A1,C001,2026/11/02,P1,1,100\r\nA2,C002,2026/11/03,P2,2,200\r\n");
        assertEquals(0, Batch.run(csv, db, log));
        assertEquals(2, count());
    }

    @Test
    public void エラー行はスキップしてログに残る() throws Exception {
        Path csv = writeCsv("A1,C001,2026/11/02,P1,1,100\r\nA2,,2026/11/03,P2,2,200\r\n");
        Batch.run(csv, db, log);
        assertEquals(1, count());
        String content = Files.readString(log, StandardCharsets.UTF_8);
        assertTrue(content.contains("3行目"));
        assertTrue(content.contains("customer_code"));
    }

    @Test
    public void 日付はISO形式で保存される() throws Exception {
        Path csv = writeCsv("A1,C001,2026/11/02,P1,1,100\r\n");
        Batch.run(csv, db, log);
        try (Connection conn = Repository.connect(db);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT order_date FROM orders")) {
            rs.next();
            assertEquals("2026-11-02", rs.getString(1));
        }
    }
}
