# libwebp-ffm

Panama FFM (jextract) bindings for the complete public **libwebp 1.6.0** API,
with bundled, from-source natives. No JNI and no user-installed libwebp.

| Artifact | Contents | License |
|---|---|---|
| `io.github.ghosthack:libwebp-ffm` | Generated FFM stubs, inline helpers, and runtime loader (JDK 22+) | MIT |
| `io.github.ghosthack:libwebp-ffm-natives` (`macos-arm64`, `windows-x64`, `linux-x64`) | `libsharpyuv`, `libwebp`, `libwebpdemux`, and `libwebpmux` | BSD-3-Clause |

The binding includes lossy and lossless encode/decode, incremental and
advanced decoding, metadata and frame demuxing, muxing, and animated WebP
encode/decode.

## Use

```xml
<dependency>
  <groupId>io.github.ghosthack</groupId>
  <artifactId>libwebp-ffm</artifactId>
  <version>1.6.0-0.1.0</version>
</dependency>
<dependency>
  <groupId>io.github.ghosthack</groupId>
  <artifactId>libwebp-ffm-natives</artifactId>
  <version>1.6.0-0.1.0</version>
  <classifier>macos-arm64</classifier> <!-- or windows-x64 / linux-x64 -->
  <scope>runtime</scope>
</dependency>
```

On the module path, declare `requires libwebp.ffm;` and run with
`--enable-native-access=libwebp.ffm`. On the class path, use
`--enable-native-access=ALL-UNNAMED`.

```java
import io.github.ghosthack.libwebpffm.libwebp.LibWebP;

try (Arena arena = Arena.ofConfined()) {
    MemorySegment width = arena.allocate(ValueLayout.JAVA_INT);
    MemorySegment height = arena.allocate(ValueLayout.JAVA_INT);
    int valid = LibWebP.WebPGetInfo(webpBytes, webpSize, width, height);

    MemorySegment rgba = arena.allocate(
            (long) width.get(ValueLayout.JAVA_INT, 0)
                    * height.get(ValueLayout.JAVA_INT, 0) * 4);
    LibWebP.WebPDecodeRGBAInto(webpBytes, webpSize, rgba,
            rgba.byteSize(), width.get(ValueLayout.JAVA_INT, 0) * 4);
}
```

`LibWebP` contains the exported native functions and constants. The top-level
`io.github.ghosthack.libwebpffm.WebP` class contains Java equivalents of
public C `static inline` helpers such as
`WebPConfigInit`, `WebPDemux`, `WebPMuxNew`, and `WebPDataClear`. Generated
struct classes such as `WebPConfig`, `WebPPicture`, and `WebPIterator` expose
layouts, allocation helpers, and field accessors.

### Library resolution

`LibWebPLibs.lookup()` resolves in this order:

1. `-Dlibwebpffm.libdir=<dir>` or `LIBWEBP_FFM_LIBDIR`;
2. the platform classifier jar, extracted once under
   `~/.cache/libwebp-ffm/<artifact-version>-<platform>/`;
3. libraries already loaded by the host application.

The override accepts a compatible system build containing libwebp and its
demux/mux libraries.

## Build

macOS arm64:

```sh
mkdir -p build
curl -sfL https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-1.6.0.tar.gz \
  | tar xz -C build
build-natives/macos-arm64.sh
jextract/gen-bindings.sh
mvn install
```

The Linux script needs CMake, Ninja, and `patchelf`. Run the Windows script
from an MSYS2 MINGW64 shell with the x86-64 toolchain, CMake, and Ninja. Native
scripts build unmodified source, disable command-line tools, stage only the
four required shared libraries, and reject unexpected runtime dependencies.

Generated sources are committed. Regeneration uses jextract 22; by default the
script uses `../ffmpeg-ffm/tools/jextract-22`, or set `JEXTRACT` explicitly.

## Licensing

The Java binding is MIT. Native classifier jars contain unmodified libwebp
under its BSD-3-Clause license and include `COPYING`, `PATENTS`, and exact
build provenance. See [THIRD-PARTY.md](THIRD-PARTY.md).
