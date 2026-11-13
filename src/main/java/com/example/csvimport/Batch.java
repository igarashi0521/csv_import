package com.example.csvimport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 受注CSV取込バッチ本体。
 *
 * <pre>
 * 使い方: java -jar target/csv-import.jar &lt;CSVファイル&gt; [--db orders.db] [--error-log import_errors.log]
 * </pre>
 */
public final class Batch {

    private Batch() {
    }

    private static Order normalize(Map<String, String> row) {
        return new Order(
                row.get("order_id").strip(),
                row.get("customer_code").strip(),
                CsvParser.parseDate(row.get("order_date").strip()),
                row.get("product_code").strip(),
                Integer.parseInt(row.get("quantity").strip()),
                Integer.parseInt(row.get("unit_price").strip()));
    }

    public static int run(Path csvPath, String dbPath, Path errorLogPath) throws IOException, SQLException {
        int okCount = 0;
        int errorCount = 0;

        try (Connection conn = Repository.connect(dbPath)) {
            conn.setAutoCommit(false);

            try (BufferedWriter log = Files.newBufferedWriter(errorLogPath, StandardCharsets.UTF_8)) {
                for (CsvParser.Row row : CsvParser.readRows(csvPath, Config.CSV_CHARSET)) {
                    List<String> errors = Validator.validateRow(row.values());
                    if (!errors.isEmpty()) {
                        errorCount++;
                        log.write(row.lineNumber() + "行目: " + String.join("; ", errors));
                        log.newLine();
                        continue;
                    }
                    Repository.insertOrder(conn, normalize(row.values()));
                    okCount++;
                }
            }

            conn.commit();
        }
        return 0;
    }

    static int execute(String[] args) throws IOException, SQLException {
        String csvPath = null;
        String dbPath = Config.DB_PATH;
        String errorLogPath = Config.ERROR_LOG_PATH;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--db" -> {
                    if (++i >= args.length) {
                        return usage();
                    }
                    dbPath = args[i];
                }
                case "--error-log" -> {
                    if (++i >= args.length) {
                        return usage();
                    }
                    errorLogPath = args[i];
                }
                default -> {
                    if (csvPath != null) {
                        return usage();
                    }
                    csvPath = args[i];
                }
            }
        }
        if (csvPath == null) {
            return usage();
        }
        return run(Path.of(csvPath), dbPath, Path.of(errorLogPath));
    }

    private static int usage() {
        System.err.println("usage: Batch <csv_path> [--db <db_path>] [--error-log <log_path>]");
        return 64;
    }

    public static void main(String[] args) throws IOException, SQLException {
        System.exit(execute(args));
    }
}
