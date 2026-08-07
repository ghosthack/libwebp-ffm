package io.github.ghosthack.libwebpffm;

import io.github.ghosthack.libwebpffm.libwebp.LibWebP;
import io.github.ghosthack.libwebpffm.libwebp.WebPBitstreamFeatures;
import io.github.ghosthack.libwebpffm.libwebp.WebPConfig;
import io.github.ghosthack.libwebpffm.libwebp.WebPData;
import io.github.ghosthack.libwebpffm.libwebp.WebPPicture;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end native load, lossless encode/decode, feature parse, demux, and mux. */
class LibWebPSmokeTest {
    @Test
    void encodesDecodesDemuxesAndMuxes() {
        int width = 32;
        int height = 24;
        long pixelBytes = (long) width * height * 4;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rgba = arena.allocate(pixelBytes);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    long offset = ((long) y * width + x) * 4;
                    rgba.set(ValueLayout.JAVA_BYTE, offset, (byte) (x * 7));
                    rgba.set(ValueLayout.JAVA_BYTE, offset + 1, (byte) (y * 9));
                    rgba.set(ValueLayout.JAVA_BYTE, offset + 2, (byte) (x ^ y));
                    rgba.set(ValueLayout.JAVA_BYTE, offset + 3, (byte) 0xff);
                }
            }

            MemorySegment encodedPointer = arena.allocate(ValueLayout.ADDRESS);
            long encodedSize = LibWebP.WebPEncodeLosslessRGBA(
                    rgba, width, height, width * 4, encodedPointer);
            assertTrue(encodedSize > 0, "lossless encode");
            MemorySegment encoded = encodedPointer.get(ValueLayout.ADDRESS, 0).reinterpret(encodedSize);
            try {
                MemorySegment parsedWidth = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment parsedHeight = arena.allocate(ValueLayout.JAVA_INT);
                assertEquals(1, LibWebP.WebPGetInfo(encoded, encodedSize, parsedWidth, parsedHeight));
                assertEquals(width, parsedWidth.get(ValueLayout.JAVA_INT, 0));
                assertEquals(height, parsedHeight.get(ValueLayout.JAVA_INT, 0));

                MemorySegment features = WebPBitstreamFeatures.allocate(arena);
                assertEquals(LibWebP.VP8_STATUS_OK(), WebP.WebPGetFeatures(encoded, encodedSize, features));
                assertEquals(width, WebPBitstreamFeatures.width(features));
                assertEquals(height, WebPBitstreamFeatures.height(features));
                assertEquals(2, WebPBitstreamFeatures.format(features), "lossless bitstream");

                MemorySegment decoded = arena.allocate(pixelBytes);
                assertNotEquals(MemorySegment.NULL, LibWebP.WebPDecodeRGBAInto(
                        encoded, encodedSize, decoded, pixelBytes, width * 4));
                assertEquals(-1, rgba.mismatch(decoded), "lossless pixels round-trip");

                MemorySegment data = WebPData.allocate(arena);
                WebP.WebPDataInit(data);
                WebPData.bytes(data, encoded);
                WebPData.size(data, encodedSize);

                MemorySegment copiedData = WebPData.allocate(arena);
                assertEquals(1, WebP.WebPDataCopy(data, copiedData));
                try {
                    assertEquals(encodedSize, WebPData.size(copiedData));
                    assertEquals(-1, encoded.mismatch(
                            WebPData.bytes(copiedData).reinterpret(encodedSize)));
                } finally {
                    WebP.WebPDataClear(copiedData);
                }

                MemorySegment demuxer = WebP.WebPDemux(data);
                assertNotEquals(MemorySegment.NULL, demuxer, "demuxer");
                try {
                    assertEquals(width, LibWebP.WebPDemuxGetI(demuxer, LibWebP.WEBP_FF_CANVAS_WIDTH()));
                    assertEquals(height, LibWebP.WebPDemuxGetI(demuxer, LibWebP.WEBP_FF_CANVAS_HEIGHT()));
                    assertEquals(1, LibWebP.WebPDemuxGetI(demuxer, LibWebP.WEBP_FF_FRAME_COUNT()));
                } finally {
                    LibWebP.WebPDemuxDelete(demuxer);
                }

                MemorySegment mux = WebP.WebPMuxCreate(data, 1);
                assertNotEquals(MemorySegment.NULL, mux, "mux");
                MemorySegment assembled = WebPData.allocate(arena);
                WebP.WebPDataInit(assembled);
                try {
                    assertEquals(LibWebP.WEBP_MUX_OK(), LibWebP.WebPMuxAssemble(mux, assembled));
                    assertTrue(WebPData.size(assembled) > 0);
                } finally {
                    WebP.WebPDataClear(assembled);
                    LibWebP.WebPMuxDelete(mux);
                }
            } finally {
                LibWebP.WebPFree(encoded);
            }
        }
    }

    @Test
    void initializesAdvancedEncoderStructuresAndReportsVersions() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment config = WebPConfig.allocate(arena);
            MemorySegment picture = WebPPicture.allocate(arena);
            assertEquals(1, WebP.WebPConfigInit(config));
            assertEquals(1, LibWebP.WebPValidateConfig(config));
            assertEquals(1, WebP.WebPPictureInit(picture));
            assertTrue(LibWebP.WebPGetDecoderVersion() > 0);
            assertTrue(LibWebP.WebPGetEncoderVersion() > 0);
            assertTrue(LibWebP.WebPGetDemuxVersion() > 0);
            assertTrue(LibWebP.WebPGetMuxVersion() > 0);
            assertTrue(WebP.WebPIsAlphaMode(LibWebP.MODE_RGBA()));
            assertFalse(WebP.WebPIsAlphaMode(LibWebP.MODE_RGB()));
        }
    }
}
