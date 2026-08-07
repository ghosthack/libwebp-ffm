#!/bin/sh
# Generate the complete public libwebp codec/mux/demux/animation FFM surface,
# then point the generated symbol lookup at LibWebPLibs.
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JX="${JEXTRACT:-$ROOT/../ffmpeg-ffm/tools/jextract-22/bin/jextract}"
PREFIX="${LIBWEBP_PREFIX:-$ROOT/build/prefix-macos-arm64}"
OUT="$ROOT/core/src/main/java"
PKG="io.github.ghosthack.libwebpffm.libwebp"
SDK="$(xcrun --show-sdk-path)"
GEN_DIR="$OUT/$(echo "$PKG" | tr . /)"
DUMP="$ROOT/build/jextract-includes.txt"
FILTERS="$ROOT/build/jextract-filters.txt"

[ -x "$JX" ] || { echo "jextract not found at $JX (set JEXTRACT=)"; exit 1; }
[ -d "$PREFIX/include/webp" ] || { echo "run build-natives/macos-arm64.sh first (or set LIBWEBP_PREFIX)"; exit 1; }
mkdir -p "$ROOT/build"

# Ask jextract which declarations originate in libwebp's own public headers,
# then turn that report into exact include filters. This keeps system headers
# out while automatically covering the complete public API for this version.
"$JX" -I "$SDK/usr/include" -I "$PREFIX/include" \
  --dump-includes "$DUMP" "$ROOT/jextract/libwebp_api.h"
awk '
  /^#### Extracted from:/ { webp = ($0 ~ /\/include\/webp\//); next }
  webp && /^--include-/ { print $1, $2 }
' "$DUMP" > "$FILTERS"
[ -s "$FILTERS" ] || { echo "no public libwebp declarations discovered"; exit 1; }

rm -rf "$GEN_DIR"
set -- --output "$OUT" -t "$PKG" --header-class-name LibWebP \
  -I "$SDK/usr/include" -I "$PREFIX/include" \
  -l ":$PREFIX/lib/libsharpyuv.dylib" \
  -l ":$PREFIX/lib/libwebp.dylib" \
  -l ":$PREFIX/lib/libwebpdemux.dylib" \
  -l ":$PREFIX/lib/libwebpmux.dylib"
while read -r option symbol; do
  set -- "$@" "$option" "$symbol"
done < "$FILTERS"
"$JX" "$@" "$ROOT/jextract/libwebp_api.h"

perl -0777 -pi -e \
  's/static final SymbolLookup SYMBOL_LOOKUP = .*?;/static final SymbolLookup SYMBOL_LOOKUP = io.github.ghosthack.libwebpffm.LibWebPLibs.lookup();/s' \
  "$GEN_DIR/LibWebP.java"

if grep -rn "$PREFIX" "$GEN_DIR" >/dev/null; then
  echo "ERROR: build-prefix paths survived in generated stubs"; exit 1
fi
grep -q "LibWebPLibs.lookup()" "$GEN_DIR/LibWebP.java" || { echo "ERROR: lookup patch did not apply"; exit 1; }
echo "Generated $(find "$GEN_DIR" -type f | wc -l | tr -d ' ') files into $GEN_DIR"

