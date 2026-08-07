#!/bin/sh
# Build libwebp from source and stage relocatable Linux x86-64 shared objects.
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${LIBWEBP_VERSION:-1.6.0}"
SRC="${1:-$ROOT/build/libwebp-$VERSION}"
PREFIX="$ROOT/build/prefix-linux-x64"
BUILD="$SRC/build-linux-x64"
DEST="$ROOT/natives/src/main/resources/libwebp-natives/linux-x64"

cmake -S "$SRC" -B "$BUILD" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX="$PREFIX" \
  -DCMAKE_INSTALL_LIBDIR=lib -DBUILD_SHARED_LIBS=ON \
  -DWEBP_BUILD_ANIM_UTILS=OFF -DWEBP_BUILD_CWEBP=OFF \
  -DWEBP_BUILD_DWEBP=OFF -DWEBP_BUILD_GIF2WEBP=OFF \
  -DWEBP_BUILD_IMG2WEBP=OFF -DWEBP_BUILD_VWEBP=OFF \
  -DWEBP_BUILD_WEBPINFO=OFF -DWEBP_BUILD_WEBPMUX=OFF \
  -DWEBP_BUILD_EXTRAS=OFF
ninja -C "$BUILD"
rm -rf "$PREFIX"
ninja -C "$BUILD" install

rm -rf "$DEST"
mkdir -p "$DEST"
: > "$DEST/manifest.txt"
for lib in libsharpyuv libwebp libwebpdemux libwebpmux; do
  shared="$(readlink -f "$PREFIX/lib/$lib.so")"
  base="$(patchelf --print-soname "$shared")"
  cp "$shared" "$DEST/$base"
  chmod 644 "$DEST/$base"
  patchelf --set-rpath '$ORIGIN' "$DEST/$base"
  echo "$base" >> "$DEST/manifest.txt"
done

for shared in "$DEST"/*.so*; do
  if readelf -d "$shared" | awk '/NEEDED/ {print $NF}' | tr -d '[]' \
      | grep -vE '^(lib(webp|sharpyuv)|libc|libm|libpthread|libdl|librt)\.so|^ld-linux'; then
    echo "ERROR: $shared links unexpected libraries (above)"; exit 1
  fi
done
cp "$SRC/COPYING" "$DEST/COPYING"
cp "$SRC/PATENTS" "$DEST/PATENTS"
{
  echo "libwebp $VERSION (linux-x64), built from unmodified source"
  echo "Source: https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-$VERSION.tar.gz"
  echo "License: BSD-3-Clause (see COPYING and PATENTS)"
  echo "CMake: Release, shared libraries, tools disabled"
} > "$DEST/BUILD-INFO.txt"
echo "Staged $(find "$DEST" -type f | wc -l | tr -d ' ') files into $DEST"
