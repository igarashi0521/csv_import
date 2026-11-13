package com.example.csvimport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 月次の受注集計レポート(取込済みデータを参照する別機能)。
 *
 * <pre>
 * 使い方: java -cp target/csv-import.jar com.example.csvimport.Report 202611 [--db orders.db]
 * </pre>
 */
public final class Report {

    private Report() {
    }

    /** 顧客別の受注金額と、曜日別(0=月 ... 6=日)の受注件数。 */
    public record Summary(Map<String, Long> amountByCustomer, Map<Integer, Integer> countByWeekday) {
    }

    public static Summary monthlySummary(Connection conn, String yyyymm) throws SQLException {
        Map<String, Long> amountByCustomer = new TreeMap<>();
        Map<Integer, Integer> countByWeekday = new TreeMap<>();

        for (Order order : Repository.fetchOrdersByMonth(conn, yyyymm)) {
            long amount = (long) order.quantity() * order.unitPrice();
            amountByCustomer.merge(order.customerCode(), amount, Long::sum);
            int weekday = order.orderDate().getDayOfWeek().getValue() - 1; // 月=0 ... 日=6
            countByWeekday.merge(weekday, 1, Integer::sum);
        }
        return new Summary(amountByCustomer, countByWeekday);
    }

    public static int execute(String[] args) throws SQLException {
        String yyyymm = null;
        String dbPath = Config.DB_PATH;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--db")) {
                if (++i >= args.length) {
                    return usage();
                }
                dbPath = args[i];
            } else if (yyyymm == null) {
                yyyymm = args[i];
            } else {
                return usage();
            }
        }
        if (yyyymm == null || !yyyymm.matches("\\d{6}")) {
            return usage();
        }

        Summary summary;
        try (Connection conn = Repository.connect(dbPath)) {
            summary = monthlySummary(conn, yyyymm);
        }

        System.out.println("=== " + yyyymm + " 顧客別受注金額 ===");
        summary.amountByCustomer().forEach((customer, amount) ->
                System.out.println(customer + "\t" + String.format(Locale.ROOT, "%,d", amount)));
        System.out.println("=== 曜日別受注件数(0=月 ... 6=日) ===");
        summary.countByWeekday().forEach((weekday, count) ->
                System.out.println(weekday + "\t" + count));
        return 0;
    }

    private static int usage() {
        System.err.println("usage: Report <yyyymm> [--db <db_path>]");
        return 64;
    }

    public static void main(String[] args) throws SQLException {
        System.exit(execute(args));
    }
}
