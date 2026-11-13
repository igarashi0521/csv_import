import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 研修用サンプルCSVを sample_data/ に生成する(ビルド不要・JDKのみで実行できる)。
 *
 * <pre>
 * java tools/GenerateSamples.java
 * </pre>
 *
 * 生成されるファイル:
 *   orders_ok.csv            正常データ(CP932)
 *   orders_with_errors.csv   エラー行を含むデータ(CP932)
 *   orders_duplicate.csv     order_id が重複しているデータ(CP932)  ※現行バッチでは異常終了する
 *   orders_utf8_bom.csv      UTF-8(BOM付き)のデータ              ※現行バッチでは読めない
 */
public class GenerateSamples {

    private static final String HEADER = "order_id,customer_code,order_date,product_code,quantity,unit_price";

    public static void main(String[] args) throws IOException {
        Charset cp932 = Charset.forName("windows-31j");
        Path dir = Path.of("sample_data");
        Files.createDirectories(dir);

        write(dir.resolve("orders_ok.csv"), cp932, List.of(
                "A0001,C001,2026/11/02,P100,10,500",
                "A0002,C002,2026/11/02,P200,3,1200",
                "A0003,C001,2026/11/03,P100,5,500",
                "A0004,C003,2026/11/04,P300,1,98000",
                "A0005,C002,2026/11/05,P200,20,1150"));

        write(dir.resolve("orders_with_errors.csv"), cp932, List.of(
                "B0001,C001,2026/11/02,P100,10,500",      // 正常
                "B0002,,2026/11/02,P200,3,1200",          // customer_code 空
                "B0003,C001,2026-11-03,P100,5,500",       // 日付形式が違う
                "B0004,C003,2026/11/04,P300,abc,98000",   // quantity が数値でない
                "B0005,C002,2026/02/30,P200,20,1150",     // 存在しない日付
                "B0006,C002,2026/11/05,P200,0,1150",      // quantity が 0
                "B0007,C004,2026/11/06,P400,100000,10",   // quantity が上限超過
                "B0008,C004,2026/11/06,P400,2,-10",       // 単価がマイナス
                "B0009,C005,2026/11/07,P500,4,2500"));    // 正常

        write(dir.resolve("orders_duplicate.csv"), cp932, List.of(
                "A0001,C001,2026/11/02,P100,10,500",      // orders_ok.csv に取込済みの order_id
                "D0001,C006,2026/11/09,P600,7,300",
                "D0001,C006,2026/11/09,P600,8,300"));     // ファイル内でも重複

        // UTF-8 BOM付き。BOM(EF BB BF)を自分で先頭に付ける
        write(dir.resolve("orders_utf8_bom.csv"), StandardCharsets.UTF_8, true, List.of(
                "E0001,C007,2026/11/10,P700,2,4000",
                "E0002,C007,2026/11/11,P701,6,150"));
    }

    private static void write(Path path, Charset charset, List<String> rows) throws IOException {
        write(path, charset, false, rows);
    }

    private static void write(Path path, Charset charset, boolean bom, List<String> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.addAll(rows);
        byte[] body = (String.join("\r\n", lines) + "\r\n").getBytes(charset);
        if (bom) {
            byte[] withBom = new byte[body.length + 3];
            withBom[0] = (byte) 0xEF;
            withBom[1] = (byte) 0xBB;
            withBom[2] = (byte) 0xBF;
            System.arraycopy(body, 0, withBom, 3, body.length);
            body = withBom;
        }
        Files.write(path, body);
        System.out.println("generated: " + path);
    }
}
