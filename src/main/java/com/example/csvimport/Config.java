package com.example.csvimport;

import java.nio.charset.Charset;

/** 設定値。研修用に最小限にしている。 */
public final class Config {

    private Config() {
    }

    public static final String DB_PATH = "orders.db";
    public static final String ERROR_LOG_PATH = "import_errors.log";

    /** 取込対象CSVの文字コード(基幹システムの出力仕様に合わせて固定)。CP932 = windows-31j。 */
    public static final Charset CSV_CHARSET = Charset.forName("windows-31j");

    /** 数量の上限(これを超える数量は入力ミスとみなす)。 */
    public static final int MAX_QUANTITY = 9999;
}
