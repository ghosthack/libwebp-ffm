# tools/

jextract itself is not committed. `jextract/gen-bindings.sh` uses the sibling
`../ffmpeg-ffm/tools/jextract-22` installation by default. To keep a separate
copy, download jextract 22 and run:

```sh
JEXTRACT=/path/to/jextract-22/bin/jextract jextract/gen-bindings.sh
```

jextract 22 targets the finalized Foreign Function & Memory API, so generated
sources compile on JDK 22 and newer. Stub generation runs on macOS; the public
libwebp layouts used here are platform-portable.

