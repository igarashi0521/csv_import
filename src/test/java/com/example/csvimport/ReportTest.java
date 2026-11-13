package com.example.csvimport;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportTest {

    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = Repository.connect(":memory:");
        Repository.insertOrder(conn, new Order("A1", "C001", LocalDate.of(2026, 11, 2), "P1", 10, 500)); // 月曜
        Repository.insertOrder(conn, new Order("A2", "C001", LocalDate.of(2026, 11, 3), "P1", 5, 500));  // 火曜
        Repository.insertOrder(conn, new Order("A3", "C002", LocalDate.of(2026, 11, 9), "P2", 2, 1000)); // 月曜
        Repository.insertOrder(conn, new Order("A4", "C002", LocalDate.of(2026, 10, 31), "P2", 9, 1000)); // 対象外(10月)
    }

    @After
    public void tearDown() throws Exception {
        conn.close();
    }

    @Test
    public void 顧客別金額を集計できる() throws Exception {
        Report.Summary summary = Report.monthlySummary(conn, "202611");
        assertEquals(Map.of("C001", 7500L, "C002", 2000L), summary.amountByCustomer());
    }

    @Test
    public void 曜日別件数を集計できる() throws Exception {
        Report.Summary summary = Report.monthlySummary(conn, "202611");
        assertEquals(Map.of(0, 2, 1, 1), summary.countByWeekday());
    }

    @Test
    public void 対象月にデータがなければ空() throws Exception {
        Report.Summary summary = Report.monthlySummary(conn, "202612");
        assertEquals(Map.of(), summary.amountByCustomer());
        assertEquals(Map.of(), summary.countByWeekday());
    }
}
