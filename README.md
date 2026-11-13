# 生成AI活用研修 ハンズオン資材:CSV取込バッチの改修(Java)

中堅社員研修(生成AI活用)のハンズオン題材です。受注CSVをDBに取り込む小さなバッチを題材に、
**既存ソース解析 → 要件整理 → 実装方針比較 → 実装 → テスト → 差分レビュー** の一連の流れを、AIと協働して体験します。

> 4時間では深掘りせず、一連の流れを体験することを重視します。
> 「AIに何を任せ、何を人が判断・確認するか」を体験することが目的です。

## 研修の位置づけ

| 項目 | 内容 |
|---|---|
| 対象 | 4〜8年目の中堅社員 |
| AIパート | 約4時間 |
| 題材 | CSV取込バッチの改修(Java 17、Maven、SQLite) |
| 重視すること | 開発プロセス全体の体験 / 「指示・比較・検証」 / 判断と確認のプロセス |

## ディレクトリ構成

```
.
├── README.md
├── pom.xml                      Maven設定(sqlite-jdbc、JUnit 4)
├── src/main/java/com/example/csvimport/   現行の取込バッチ(改修対象)
│   ├── Batch.java                 取込バッチ本体(main)
│   ├── CsvParser.java             CSV読込・日付変換
│   ├── Validator.java             1行ごとの検証
│   ├── Repository.java            SQLite(受注テーブル)アクセス(JDBC)
│   ├── Order.java                 受注1件(record)
│   ├── Report.java                月次集計(取込済みデータを参照する別機能)
│   └── Config.java                設定値
├── src/test/java/com/example/csvimport/   現行の単体テスト(JUnit 4)
├── scripts/run_nightly.sh       運用側の夜間ジョブ(終了コードに依存)
├── sample_data/                 サンプルCSV(tools/GenerateSamples.java で再生成可)
├── tools/GenerateSamples.java   サンプルCSV生成(ビルド不要)
└── docs/
    ├── 00_instructor_guide.md         講師用ガイド(タイムテーブル・進め方・想定解)
    ├── 01_change_request.md           改修依頼書(受講者に渡す題材)
    ├── 02_handson1_requirements.md    ハンズオン①:要件整理・設計
    ├── 03_handson2_implement_test.md  ハンズオン②:実装・テスト
    ├── 04_handson3_impact_analysis.md ハンズオン③:既存ソース解析・変更影響
    ├── 05_diff_review_checklist.md    差分レビュー観点
    ├── 06_prompt_examples.md          「指示・比較・検証」プロンプト例
    └── 07_reflection_and_next.md      振り返りシート・研修後の導線
```

## セットアップ

JDK 17 以降と Maven 3.9 以降が必要です。初回のビルド時に Maven Central から依存ライブラリを取得します。

```sh
mvn test           # 現行の単体テストを実行(全件成功する)
mvn package        # target/csv-import.jar と target/lib/ を作成

java -jar target/csv-import.jar sample_data/orders_ok.csv
java -cp target/csv-import.jar com.example.csvimport.Report 202611
```

サンプルCSVは `sample_data/` に生成済みです。作り直す場合は `java tools/GenerateSamples.java` を実行してください。
実行したディレクトリに `orders.db`(DB)と `import_errors.log`(エラー行ログ)が作られます(`--db`、`--error-log` で変更可)。

## 現行バッチの仕様(要約)

- 入力:受注CSV(CP932、ヘッダーあり)。列は `order_id, customer_code, order_date, product_code, quantity, unit_price`
- 1行ごとに検証し、エラー行は `import_errors.log` に出力してスキップ、正常行をSQLiteへ登録
- 日付は `YYYY/MM/DD` で受け取り、DBには `YYYY-MM-DD` で保存
- 登録は最後に1回コミットする(途中で例外が出ると、そのファイルの分は全て未登録になる)
- 終了コードは常に 0

## 現行バッチの既知の課題(ハンズオンで扱う)

サンプルCSVで再現できます。

| 再現ファイル | 現象 |
|---|---|
| `orders_duplicate.csv`(`orders_ok.csv` 取込後) | `order_id` 重複で `SQLException`(主キー制約違反)となり、**そのファイルの取込分が全て未登録になる** |
| `orders_utf8_bom.csv` | `MalformedInputException` で読み込めない |
| (全般) | 取込結果の件数が分からない / エラーがあっても終了コードが 0 |

詳細は `docs/01_change_request.md` を参照してください。

## 補足

- 実行時に日本語の出力(`Report`)が文字化けする場合は、`java -Dstdout.encoding=UTF-8 ...`(JDK 19 以降)を指定するか、端末の文字コードを確認してください。
- SQLiteのJDBCドライバ(`sqlite-jdbc`)はネイティブライブラリを内蔵しており、別途のDBインストールは不要です。
