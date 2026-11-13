package com.example.csvimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ValidatorTest {

    private static Map<String, String> validRow() {
        Map<String, String> row = new HashMap<>();
        row.put("order_id", "A0001");
        row.put("customer_code", "C001");
        row.put("order_date", "2026/11/02");
        row.put("product_code", "P100");
        row.put("quantity", "10");
        row.put("unit_price", "500");
        return row;
    }

    private static List<String> validate(String key, String value) {
        Map<String, String> row = validRow();
        row.put(key, value);
        return Validator.validateRow(row);
    }

    @Test
    public void 正常な行はエラーなし() {
        assertEquals(List.of(), Validator.validateRow(validRow()));
    }

    @Test
    public void 必須項目が空ならエラー() {
        for (String key : List.of("order_id", "customer_code", "product_code")) {
            assertEquals(key, 1, validate(key, "").size());
        }
    }

    @Test
    public void 日付形式が違うとエラー() {
        assertEquals(1, validate("order_date", "2026-11-02").size());
    }

    @Test
    public void 存在しない日付はエラー() {
        assertEquals(1, validate("order_date", "2026/02/30").size());
    }

    @Test
    public void 数量が数値でないとエラー() {
        assertEquals(1, validate("quantity", "abc").size());
    }

    @Test
    public void 数量が0以下はエラー() {
        assertEquals(1, validate("quantity", "0").size());
    }

    @Test
    public void 数量が上限超過はエラー() {
        assertEquals(1, validate("quantity", "10000").size());
        assertEquals(List.of(), validate("quantity", "9999"));
    }

    @Test
    public void 単価がマイナスはエラー() {
        assertEquals(1, validate("unit_price", "-1").size());
    }

    @Test
    public void 列が欠けていてもnullで落ちない() {
        Map<String, String> row = new HashMap<>();
        row.put("order_id", "A0001");
        assertTrue(Validator.validateRow(row).size() >= 1);
    }
}
