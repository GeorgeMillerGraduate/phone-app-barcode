package com.jengacode.barcodescanner.generator;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import java.util.*;

public final class BarcodeGenerator {
  public static final String[] FORMATS = {
    "QR_CODE",
    "CODE_128",
    "EAN_13",
    "EAN_8",
    "UPC_A",
    "UPC_E",
    "CODE_39",
    "CODE_93",
    "ITF",
    "CODABAR"
  };

  private BarcodeGenerator() {}

  public static BitMatrix generate(String value, String format, int width, int height)
      throws WriterException {
    if (value.isEmpty()) throw new IllegalArgumentException("Enter some text or a barcode value");
    if (width < 128 || width > 2048 || height < 128 || height > 2048)
      throw new IllegalArgumentException("Dimensions must be between 128 and 2048 pixels");
    BarcodeFormat type = BarcodeFormat.valueOf(format);
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, type == BarcodeFormat.QR_CODE ? 4 : 20);
    if (type == BarcodeFormat.QR_CODE) hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    return new MultiFormatWriter().encode(value, type, width, height, hints);
  }
}
