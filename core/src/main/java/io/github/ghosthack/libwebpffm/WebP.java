package io.github.ghosthack.libwebpffm;

import io.github.ghosthack.libwebpffm.libwebp.LibWebP;
import io.github.ghosthack.libwebpffm.libwebp.WebPData;

import java.lang.foreign.MemorySegment;

/**
 * Java equivalents of libwebp's public {@code static inline} convenience
 * functions. The exported native entry points remain in {@link LibWebP}.
 */
public final class WebP {
    private WebP() {}

    public static int WebPInitDecBuffer(MemorySegment buffer) {
        return LibWebP.WebPInitDecBufferInternal(buffer, LibWebP.WEBP_DECODER_ABI_VERSION());
    }

    public static int WebPInitDecoderConfig(MemorySegment config) {
        return LibWebP.WebPInitDecoderConfigInternal(config, LibWebP.WEBP_DECODER_ABI_VERSION());
    }

    public static int WebPGetFeatures(MemorySegment data, long size, MemorySegment features) {
        return LibWebP.WebPGetFeaturesInternal(data, size, features, LibWebP.WEBP_DECODER_ABI_VERSION());
    }

    public static int WebPConfigInit(MemorySegment config) {
        return WebPConfigPreset(config, LibWebP.WEBP_PRESET_DEFAULT(), 75.0f);
    }

    public static int WebPConfigPreset(MemorySegment config, int preset, float quality) {
        return LibWebP.WebPConfigInitInternal(config, preset, quality, LibWebP.WEBP_ENCODER_ABI_VERSION());
    }

    public static int WebPPictureInit(MemorySegment picture) {
        return LibWebP.WebPPictureInitInternal(picture, LibWebP.WEBP_ENCODER_ABI_VERSION());
    }

    public static void WebPDataInit(MemorySegment data) {
        if (!data.equals(MemorySegment.NULL)) {
            data.asSlice(0, WebPData.layout().byteSize()).fill((byte) 0);
        }
    }

    public static void WebPDataClear(MemorySegment data) {
        if (!data.equals(MemorySegment.NULL)) {
            MemorySegment bytes = WebPData.bytes(data);
            if (!bytes.equals(MemorySegment.NULL)) LibWebP.WebPFree(bytes);
            WebPDataInit(data);
        }
    }

    public static int WebPDataCopy(MemorySegment source, MemorySegment destination) {
        if (source.equals(MemorySegment.NULL) || destination.equals(MemorySegment.NULL)) return 0;
        WebPDataInit(destination);
        long size = WebPData.size(source);
        MemorySegment bytes = WebPData.bytes(source);
        if (!bytes.equals(MemorySegment.NULL) && size != 0) {
            MemorySegment copy = LibWebP.WebPMalloc(size);
            if (copy.equals(MemorySegment.NULL)) return 0;
            MemorySegment.copy(bytes.reinterpret(size), 0, copy.reinterpret(size), 0, size);
            WebPData.bytes(destination, copy);
            WebPData.size(destination, size);
        }
        return 1;
    }

    public static MemorySegment WebPDemux(MemorySegment data) {
        return LibWebP.WebPDemuxInternal(data, 0, MemorySegment.NULL, LibWebP.WEBP_DEMUX_ABI_VERSION());
    }

    public static MemorySegment WebPDemuxPartial(MemorySegment data, MemorySegment state) {
        return LibWebP.WebPDemuxInternal(data, 1, state, LibWebP.WEBP_DEMUX_ABI_VERSION());
    }

    public static int WebPAnimDecoderOptionsInit(MemorySegment options) {
        return LibWebP.WebPAnimDecoderOptionsInitInternal(options, LibWebP.WEBP_DEMUX_ABI_VERSION());
    }

    public static MemorySegment WebPAnimDecoderNew(MemorySegment data, MemorySegment options) {
        return LibWebP.WebPAnimDecoderNewInternal(data, options, LibWebP.WEBP_DEMUX_ABI_VERSION());
    }

    public static MemorySegment WebPMuxNew() {
        return LibWebP.WebPNewInternal(LibWebP.WEBP_MUX_ABI_VERSION());
    }

    public static MemorySegment WebPMuxCreate(MemorySegment bitstream, int copyData) {
        return LibWebP.WebPMuxCreateInternal(bitstream, copyData, LibWebP.WEBP_MUX_ABI_VERSION());
    }

    public static int WebPAnimEncoderOptionsInit(MemorySegment options) {
        return LibWebP.WebPAnimEncoderOptionsInitInternal(options, LibWebP.WEBP_MUX_ABI_VERSION());
    }

    public static MemorySegment WebPAnimEncoderNew(int width, int height, MemorySegment options) {
        return LibWebP.WebPAnimEncoderNewInternal(width, height, options, LibWebP.WEBP_MUX_ABI_VERSION());
    }

    public static boolean WebPIsPremultipliedMode(int mode) {
        return mode == LibWebP.MODE_rgbA() || mode == LibWebP.MODE_bgrA()
                || mode == LibWebP.MODE_Argb() || mode == LibWebP.MODE_rgbA_4444();
    }

    public static boolean WebPIsAlphaMode(int mode) {
        return mode == LibWebP.MODE_RGBA() || mode == LibWebP.MODE_BGRA()
                || mode == LibWebP.MODE_ARGB() || mode == LibWebP.MODE_RGBA_4444()
                || mode == LibWebP.MODE_YUVA() || WebPIsPremultipliedMode(mode);
    }

    public static boolean WebPIsRGBMode(int mode) {
        return mode < LibWebP.MODE_YUV();
    }
}
