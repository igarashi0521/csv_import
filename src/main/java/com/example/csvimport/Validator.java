package com.example.csvimport;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 1行分のデータを検証し、エラーメッセージの一覧を返す。 */
public final class Validator {

    private Validator() {
    }

    private static String get(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null ? "" : value.strip();
    }

    /** エラーがなければ空のリストを返す。 */
    public static List<String> validateRow(Map<String, String> row) {
        List<String> errors = new ArrayList<>();

        if (get(row, "order_id").isEmpty()) {
            errors.add("order_id が空です");
        }
        if (get(row, "customer_code").isEmpty()) {
            errors.add("customer_code が空です");
        }
        if (get(row, "product_code").isEmpty()) {
            errors.add("product_code が空です");
        }

        try {
            CsvParser.parseDate(get(row, "order_date"));
        } catch (DateTimeParseException e) {
            errors.add("order_date が YYYY/MM/DD 形式の正しい日付ではありません");
        }

        try {
            int quantity = Integer.parseInt(get(row, "quantity"));
            if (quantity <= 0) {
                errors.add("quantity は1以上にしてください");
            } else if (quantity > Config.MAX_QUANTITY) {
                errors.add("quantity が上限(" + Config.MAX_QUANTITY + ")を超えています");
            }
        } catch (NumberFormatException e) {
            errors.add("quantity が整数ではありません");
        }

        try {
            if (Integer.parseInt(get(row, "unit_price")) < 0) {
                errors.add("unit_price は0以上にしてください");
            }
        } catch (NumberFormatException e) {
            errors.add("unit_price が整数ではありません");
        }

        return errors;
    }
}
