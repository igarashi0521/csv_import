package com.example.csvimport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQLite(受注テーブル)へのアクセス。 */
public final class Repository {

    private Repository() {
    }

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS orders (
                order_id      TEXT PRIMARY KEY,
                customer_code TEXT NOT NULL,
                order_date    TEXT NOT NULL,   -- YYYY-MM-DD
                product_code  TEXT NOT NULL,
                quantity      INTEGER NOT NULL,
                unit_price    INTEGER NOT NULL
            )
            """;

    /** DBに接続し、テーブルがなければ作成する。メモリDBは ":memory:" を指定する。 */
    public static Connection connect(String dbPath) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement st = conn.createStatement()) {
            st.execute(SCHEMA);
        }
        return conn;
    }

    public static void insertOrder(Connection conn, Order order) throws SQLException {
        String sql = "INSERT INTO orders VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.orderId());
            ps.setString(2, order.customerCode());
            ps.setString(3, order.orderDate().toString()); // YYYY-MM-DD
            ps.setString(4, order.productCode());
            ps.setInt(5, order.quantity());
            ps.setInt(6, order.unitPrice());
            ps.executeUpdate();
        }
    }

    /** '202611' のような年月で受注を取得する。 */
    public static List<Order> fetchOrdersByMonth(Connection conn, String yyyymm) throws SQLException {
        String prefix = yyyymm.substring(0, 4) + "-" + yyyymm.substring(4, 6) + "-%";
        String sql = "SELECT order_id, customer_code, order_date, product_code, quantity, unit_price "
                + "FROM orders WHERE order_date LIKE ? ORDER BY order_date, order_id";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(new Order(
                            rs.getString("order_id"),
                            rs.getString("customer_code"),
                            LocalDate.parse(rs.getString("order_date")),
                            rs.getString("product_code"),
                            rs.getInt("quantity"),
                            rs.getInt("unit_price")));
                }
            }
        }
        return orders;
    }
}
