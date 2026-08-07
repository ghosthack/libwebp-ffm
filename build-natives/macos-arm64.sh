#!/bin/sh
# Build libwebp from source and stage relocatable macOS arm64 dylibs.
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${LIBWEBP_VERSION:-1.6.0}"
SRC="${1:-$ROOT/build/libwebp-$VERSION}"
PREFIX="$ROOT/build/prefix-macos-arm64"
BUILD="$SRC/build-macos-arm64"
DEST="$ROOT/natives/src/main/resources/libwebp-natives/macos-arm64"

cmake -S "$SRC" -B "$BUILD" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_INSTALL_PREFIX="$PREFIX" \
  -DCMAKE_INSTALL_LIBDIR=lib \
  -DCMAKE_INSTALL_NAME_DIR='@loader_path' \
  -DBUILD_SHARED_LIBS=ON \
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
  dylib="$PREFIX/lib/$lib.dylib"
  install_name="$(otool -D "$dylib" | tail -1)"
  base="$(basename "$install_name")"
  cp "$(cd "$PREFIX/lib" && readlink -f "$lib.dylib")" "$DEST/$base"
  chmod 644 "$DEST/$base"
  if otool -l "$DEST/$base" | grep -Fq "path $PREFIX/lib"; then
    install_name_tool -delete_rpath "$PREFIX/lib" "$DEST/$base"
  fi
  echo "$base" >> "$DEST/manifest.txt"
done

for dylib in "$DEST"/*.dylib; do
  if otool -L "$dylib" | tail -n +2 | grep -vE '@loader_path/|/usr/lib/|/System/Library/'; then
    echo "ERROR: $dylib has non-relocatable dependencies (above)"; exit 1
  fi
  if otool -l "$dylib" | tail -n +2 | grep -F "$ROOT"; then
    echo "ERROR: $dylib contains a build-tree load path"; exit 1
  fi
done
cp "$SRC/COPYING" "$DEST/COPYING"
cp "$SRC/PATENTS" "$DEST/PATENTS"
{
  echo "libwebp $VERSION (macos-arm64), built from unmodified source"
  echo "Source: https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-$VERSION.tar.gz"
  echo "License: BSD-3-Clause (see COPYING and PATENTS)"
  echo "CMake: Release, shared libraries, tools disabled, @loader_path install names"
} > "$DEST/BUILD-INFO.txt"
echo "Staged $(find "$DEST" -type f | wc -l | tr -d ' ') files into $DEST"
