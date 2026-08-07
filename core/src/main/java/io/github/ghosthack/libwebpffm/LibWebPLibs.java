package io.github.ghosthack.libwebpffm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Resolves the libwebp shared libraries used by the generated bindings. */
public final class LibWebPLibs {
    private static final Arena LIBRARY_ARENA = Arena.ofAuto();
    private static volatile SymbolLookup lookup;

    private LibWebPLibs() {}

    /**
     * Returns a lookup containing libsharpyuv, libwebp, libwebpdemux, and
     * libwebpmux symbols. Resolution is lazy and cached for the JVM lifetime.
     */
    public static SymbolLookup lookup() {
        SymbolLookup current = lookup;
        if (current == null) {
            synchronized (LibWebPLibs.class) {
                current = lookup;
                if (current == null) {
                    lookup = current = resolve();
                }
            }
        }
        return current;
    }

    private static SymbolLookup resolve() {
        String override = System.getProperty("libwebpffm.libdir", System.getenv("LIBWEBP_FFM_LIBDIR"));
        SymbolLookup resolved = override != null ? fromDirectory(Path.of(override)) : fromBundle();
        SymbolLookup fallback = SymbolLookup.loaderLookup().or(Linker.nativeLinker().defaultLookup());
        return resolved != null ? resolved.or(fallback) : fallback;
    }

    private static SymbolLookup fromDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("libwebpffm.libdir does not exist: " + dir);
        }
        List<Path> libs = new ArrayList<>();
        try (var files = Files.list(dir)) {
            files.filter(LibWebPLibs::isLibWebPLibrary).sorted().forEach(libs::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (libs.isEmpty()) {
            throw new IllegalStateException("no libwebp shared libraries found in " + dir);
        }
        libs.sort((left, right) -> Integer.compare(loadRank(left), loadRank(right)));
        return chain(libs);
    }

    private static boolean isLibWebPLibrary(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        boolean nativeExtension = name.endsWith(".dylib") || name.endsWith(".dll") || name.contains(".so");
        return nativeExtension && (name.contains("libwebp") || name.startsWith("webp")
                || name.contains("sharpyuv"));
    }

    private static int loadRank(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.contains("sharpyuv")) return 0;
        if (name.contains("demux") || name.contains("mux")) return 2;
        return 1;
    }

    private static SymbolLookup fromBundle() {
        String classifier = classifier();
        String base = "libwebp-natives/" + classifier + "/";
        List<String> manifest = readManifest(base);
        if (manifest == null) {
            return null;
        }
        Path cacheDir = Path.of(System.getProperty("user.home"), ".cache", "libwebp-ffm",
                Version.VERSION + "-" + classifier);
        try {
            Files.createDirectories(cacheDir);
            List<Path> libs = new ArrayList<>();
            for (String name : manifest) {
                libs.add(extract(base + name, cacheDir.resolve(name)));
            }
            return chain(libs);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to extract bundled libwebp natives to " + cacheDir, e);
        }
    }

    private static List<String> readManifest(String base) {
        try (InputStream in = LibWebPLibs.class.getClassLoader().getResourceAsStream(base + "manifest.txt")) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim).filter(line -> !line.isEmpty()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path extract(String resource, Path target) throws IOException {
        try (InputStream in = LibWebPLibs.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IOException("missing classpath resource " + resource);
            byte[] bytes = in.readAllBytes();
            if (Files.exists(target) && Files.size(target) == bytes.length
                    && Arrays.equals(Files.readAllBytes(target), bytes)) return target;
            Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
            Files.write(temp, bytes);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        }
    }

    private static SymbolLookup chain(List<Path> libraries) {
        SymbolLookup result = null;
        for (Path library : libraries) {
            SymbolLookup next = SymbolLookup.libraryLookup(library.toAbsolutePath().toString(), LIBRARY_ARENA);
            result = result == null ? next : result.or(next);
        }
        return result;
    }

    static String classifier() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        String osPart = os.contains("mac") ? "macos" : os.contains("win") ? "windows" : "linux";
        String archPart = arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : "x64";
        return osPart + "-" + archPart;
    }
}
