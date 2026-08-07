#!/bin/sh
# Build libwebp from an MSYS2 MINGW64 shell and stage x86-64 DLLs.
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${LIBWEBP_VERSION:-1.6.0}"
SRC="${1:-$ROOT/build/libwebp-$VERSION}"
PREFIX="$ROOT/build/prefix-windows-x64"
BUILD="$SRC/build-windows-x64"
DEST="$ROOT/natives/src/main/resources/libwebp-natives/windows-x64"

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
  # libwebp's MinGW CMake build currently installs unversioned DLLs. Keep a
  # versioned-name fallback for compatible builds that encode the ABI in the
  # filename, without allowing "libwebp" to accidentally select demux/mux.
  dll="$PREFIX/bin/$lib.dll"
  if [ ! -f "$dll" ]; then
    dll="$(find "$PREFIX/bin" -maxdepth 1 -type f -iname "$lib-[0-9]*.dll" | head -1)"
  fi
  [ -n "$dll" ] && [ -f "$dll" ] || { echo "missing $lib DLL in $PREFIX/bin"; exit 1; }
  base="$(basename "$dll")"
  cp "$dll" "$DEST/$base"
  echo "$base" >> "$DEST/manifest.txt"
done
for runtime in libwinpthread-1.dll libgcc_s_seh-1.dll; do
  required=0
  for dll in "$DEST"/*.dll; do
    if objdump -p "$dll" | grep -qi "DLL Name: $runtime"; then required=1; fi
  done
  if [ "$required" -eq 1 ]; then
    [ -f "/mingw64/bin/$runtime" ] || { echo "missing required runtime /mingw64/bin/$runtime"; exit 1; }
    cp "/mingw64/bin/$runtime" "$DEST/$runtime"
    sed -i "1i$runtime" "$DEST/manifest.txt"
  fi
done

ALLOWED='lib(sharpyuv|webp)|libwinpthread|libgcc_s_seh|KERNEL32|USER32|ADVAPI32|msvcrt|ntdll|api-ms-'
for dll in "$DEST"/*.dll; do
  if objdump -p "$dll" | awk '/DLL Name:/{print $3}' | grep -viE "^($ALLOWED)"; then
    echo "ERROR: $dll imports unexpected DLLs (above)"; exit 1
  fi
done
cp "$SRC/COPYING" "$DEST/COPYING"
cp "$SRC/PATENTS" "$DEST/PATENTS"
{
  echo "libwebp $VERSION (windows-x64), built from unmodified source with MSYS2 mingw-w64"
  echo "Source: https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-$VERSION.tar.gz"
  echo "License: BSD-3-Clause (see COPYING and PATENTS)"
  echo "CMake: Release, shared libraries, tools disabled"
} > "$DEST/BUILD-INFO.txt"
echo "Staged $(find "$DEST" -type f | wc -l | tr -d ' ') files into $DEST"
