#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"
DEFAULT_MAIN_CLASS="chessViewController.Launcher"

echo "Resetting $OUT_DIR"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

ARGS_FILE=$(mktemp)
find "$SRC_DIR" \( -path "$SRC_DIR/unitTests" -o -path "$SRC_DIR/unitTests/*" \) -prune -o -name '*.java' -print > "$ARGS_FILE"

if [ ! -s "$ARGS_FILE" ]; then
  echo "No Java sources found in $SRC_DIR"
  rm "$ARGS_FILE"
  exit 1
fi

echo "Compiling sources..."
javac -d "$OUT_DIR" @"$ARGS_FILE"

rm "$ARGS_FILE"

echo "Done. Classes are in $OUT_DIR."
