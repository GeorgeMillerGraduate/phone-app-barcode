package com.jengacode.barcodescanner.scanner;

import androidx.camera.core.ImageProxy;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.nio.ByteBuffer;
import java.util.*;

public final class BarcodeDecoder {
  public static final List<BarcodeFormat> FORMATS =
      Arrays.asList(
          BarcodeFormat.EAN_13,
          BarcodeFormat.EAN_8,
          BarcodeFormat.UPC_A,
          BarcodeFormat.UPC_E,
          BarcodeFormat.CODE_128,
          BarcodeFormat.CODE_39,
          BarcodeFormat.CODE_93,
          BarcodeFormat.ITF,
          BarcodeFormat.CODABAR,
          BarcodeFormat.QR_CODE,
          BarcodeFormat.DATA_MATRIX,
          BarcodeFormat.PDF_417,
          BarcodeFormat.AZTEC);

  public Result decode(LuminanceSource source, boolean qrOnly) {
    MultiFormatReader reader = new MultiFormatReader();
    Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
    hints.put(
        DecodeHintType.POSSIBLE_FORMATS,
        qrOnly ? Collections.singletonList(BarcodeFormat.QR_CODE) : FORMATS);
    hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    reader.setHints(hints);
    try {
      return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
    } catch (NotFoundException first) {
      reader.reset();
      try {
        return reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source.invert())));
      } catch (NotFoundException ignored) {
        return null;
      }
    } finally {
      reader.reset();
    }
  }

  public Result frame(ImageProxy image, boolean qrOnly) {
    ImageProxy.PlaneProxy plane = image.getPlanes()[0];
    ByteBuffer buffer = plane.getBuffer().duplicate();
    int base = buffer.position(), w = image.getWidth(), h = image.getHeight();
    byte[] data = new byte[w * h];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        data[y * w + x] = buffer.get(base + y * plane.getRowStride() + x * plane.getPixelStride());
    int rotation = image.getImageInfo().getRotationDegrees();
    int outW = rotation == 90 || rotation == 270 ? h : w;
    int outH = rotation == 90 || rotation == 270 ? w : h;
    if (rotation != 0) data = rotate(data, w, h, rotation);
    return decode(new PlanarYUVLuminanceSource(data, outW, outH, 0, 0, outW, outH, false), qrOnly);
  }

  public static byte[] rotate(byte[] data, int w, int h, int rotation) {
    byte[] rotated = new byte[data.length];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        int index =
            switch (rotation) {
              case 90 -> x * h + (h - 1 - y);
              case 180 -> (h - 1 - y) * w + (w - 1 - x);
              case 270 -> (w - 1 - x) * h + y;
              default -> y * w + x;
            };
        rotated[index] = data[y * w + x];
      }
    return rotated;
  }
}
