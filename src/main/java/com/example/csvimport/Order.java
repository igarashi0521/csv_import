package com.example.csvimport;

import java.time.LocalDate;

/** 受注1件分。 */
public record Order(
        String orderId,
        String customerCode,
        LocalDate orderDate,
        String productCode,
        int quantity,
        int unitPrice) {
}
