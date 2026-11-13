#!/bin/sh
# 夜間ジョブ(運用側のスクリプトを想定)。
# 取込バッチが成功(終了コード0)した場合のみ、月次集計を実行する。
# 事前に `mvn package` で target/csv-import.jar を作成しておくこと。
set -e

CSV="$1"
YYYYMM="$2"

if [ -z "$CSV" ] || [ -z "$YYYYMM" ]; then
    echo "usage: $0 <csv> <yyyymm>" >&2
    exit 64
fi

JAR="$(dirname "$0")/../target/csv-import.jar"

java -jar "$JAR" "$CSV"
java -cp "$JAR" com.example.csvimport.Report "$YYYYMM"
