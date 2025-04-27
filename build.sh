#!/usr/bin/env bash
set -euo pipefail

# compile.sh — compile all Java sources (excluding unitTests) into the out/ directory, then optionally run

# Determine project root and source/output dirs
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"

# Default main class to run, if none provided
DEFAULT_MAIN_CLASS="chessViewController.Launcher"

# Ensure output directory exists
echo "Preparing output directory: $OUT_DIR"
mkdir -p "$OUT_DIR"

# Collect Java source files into a temp file, excluding unitTests
ARGS_FILE=$(mktemp)
find "$SRC_DIR" \( -path "$SRC_DIR/unitTests" -o -path "$SRC_DIR/unitTests/*" \) -prune -o -name '*.java' -print > "$ARGS_FILE"

# Abort if none found
if [ ! -s "$ARGS_FILE" ]; then
  echo "No Java source files found (excluding unitTests) in $SRC_DIR"
  rm "$ARGS_FILE"
  exit 1
fi

# Compile sources
echo "Compiling Java sources (excluding unitTests)..."
javac -d "$OUT_DIR" @"$ARGS_FILE"

# Clean up
rm "$ARGS_FILE"

echo "Compilation complete. Classes are in $OUT_DIR."
