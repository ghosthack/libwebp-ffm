# Third-party notices

## libwebp

The `libwebp-ffm-natives` classifier jars contain `libsharpyuv`, `libwebp`,
`libwebpdemux`, and `libwebpmux` built from unmodified libwebp source. libwebp
is copyright Google LLC and licensed under the BSD 3-Clause license.

- The license and patent grant ship in every classifier jar as `COPYING` and
  `PATENTS`.
- The exact version, source archive, target platform, and build configuration
  ship as `BUILD-INFO.txt`.
- Corresponding source is available from the WebM Project release archive at
  `https://storage.googleapis.com/downloads.webmproject.org/releases/webp/`.
- Users can substitute compatible shared libraries with
  `LIBWEBP_FFM_LIBDIR` or `-Dlibwebpffm.libdir`.

The FFM binding and loader code contains no libwebp source and is MIT licensed.

