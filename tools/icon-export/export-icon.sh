#!/usr/bin/env bash
#
# Membuat ulang store/ic_launcher_512.png dari geometri ikon adaptive di app.
#
# Jalankan dari mana saja:
#   bash tools/icon-export/export-icon.sh
#
# Butuh JDK apa saja (javac dan java dari PATH). Tidak ada dependensi lain dan
# tidak ada koneksi internet: file PNG-nya digambar langsung oleh java.awt.
set -euo pipefail

cd "$(dirname "$0")/../.."

BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

javac -d "$BUILD_DIR" tools/icon-export/ExportLauncherIcon.java

# 1) sama persis dengan ikon adaptive di app
java -Djava.awt.headless=true -cp "$BUILD_DIR" ExportLauncherIcon "store/ic_launcher_512.png"

# 2) pilihan dengan jejak kaki diperbesar, tetap di dalam area aman
java -Djava.awt.headless=true -cp "$BUILD_DIR" ExportLauncherIcon "store/ic_launcher_512_besar.png" large
