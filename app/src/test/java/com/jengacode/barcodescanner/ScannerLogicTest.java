package com.jengacode.barcodescanner;

import static org.junit.Assert.*;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.jengacode.barcodescanner.database.ScanRecord;
import com.jengacode.barcodescanner.generator.BarcodeGenerator;
import com.jengacode.barcodescanner.scanner.*;
import com.jengacode.barcodescanner.utils.*;
import java.util.*;
import org.junit.Test;

public class ScannerLogicTest {
  @Test
  public void rotationHandlesNonSquareFrames() {
    byte[] frame = {1, 2, 3, 4, 5, 6};
    assertArrayEquals(new byte[] {4, 1, 5, 2, 6, 3}, BarcodeDecoder.rotate(frame, 3, 2, 90));
    assertArrayEquals(new byte[] {6, 5, 4, 3, 2, 1}, BarcodeDecoder.rotate(frame, 3, 2, 180));
    assertArrayEquals(new byte[] {3, 6, 2, 5, 1, 4}, BarcodeDecoder.rotate(frame, 3, 2, 270));
  }

  @Test
  public void duplicatesArePerCodeAndExpire() {
    DuplicateGate gate = new DuplicateGate();
    assertTrue(gate.accept("qr:a", 100, 5000));
    assertFalse(gate.accept("qr:a", 200, 5000));
    assertTrue(gate.accept("qr:b", 200, 5000));
    assertTrue(gate.accept("qr:a", 5100, 5000));
  }

  @Test
  public void generatedCodesRoundTrip() throws Exception {
    String[][] codes = {
      {"QR_CODE", "https://jenga-code.com/"},
      {"CODE_128", "JENGA-001"},
      {"EAN_13", "5901234123457"},
      {"EAN_8", "96385074"},
      {"UPC_A", "036000291452"},
      {"UPC_E", "01234565"},
      {"CODE_39", "JENGA-001"},
      {"CODE_93", "JENGA-001"},
      {"ITF", "12345678"},
      {"CODABAR", "A123456B"}
    };
    for (String[] code : codes) {
      com.google.zxing.common.BitMatrix m = BarcodeGenerator.generate(code[1], code[0], 800, 400);
      int[] pixels = new int[m.getWidth() * m.getHeight()];
      for (int y = 0; y < m.getHeight(); y++)
        for (int x = 0; x < m.getWidth(); x++)
          pixels[y * m.getWidth() + x] = m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
      MultiFormatReader reader = new MultiFormatReader();
      Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
      hints.put(
          DecodeHintType.POSSIBLE_FORMATS,
          Collections.singletonList(BarcodeFormat.valueOf(code[0])));
      hints.put(DecodeHintType.TRY_HARDER, true);
      if (code[0].equals("CODABAR")) hints.put(DecodeHintType.RETURN_CODABAR_START_END, true);
      Result result =
          reader.decode(
              new BinaryBitmap(
                  new HybridBinarizer(new RGBLuminanceSource(m.getWidth(), m.getHeight(), pixels))),
              hints);
      assertEquals(code[0], code[1], result.getText());
    }
  }

  @Test
  public void exportsEscapeTextAndPreventCsvFormulaExecution() {
    assertEquals("\"'=1+1\"", ExportCodec.csvCell("=1+1"));
    ScanRecord r = new ScanRecord();
    r.value = "a\"b\n,c";
    r.note = "+SUM(A1)";
    r.format = "QR_CODE";
    String csv = ExportCodec.csv(Collections.singletonList(r));
    assertTrue(csv.contains("a\"\"b\n,c"));
    assertTrue(csv.contains("'+SUM(A1)"));
    String json = ExportCodec.json(Collections.singletonList(r));
    assertTrue(json.contains("a\\\"b\\u000a,c"));
    assertFalse(json.contains("a\"b\n,c"));
  }

  @Test
  public void scannerDecodesTwoDimensionalFormats() throws Exception {
    BarcodeDecoder decoder = new BarcodeDecoder();
    for (BarcodeFormat format :
        new BarcodeFormat[] {
          BarcodeFormat.QR_CODE,
          BarcodeFormat.DATA_MATRIX,
          BarcodeFormat.PDF_417,
          BarcodeFormat.AZTEC
        }) {
      String value = "JENGA-" + format.name();
      com.google.zxing.common.BitMatrix m = new MultiFormatWriter().encode(value, format, 800, 400);
      int[] pixels = new int[m.getWidth() * m.getHeight()];
      for (int y = 0; y < m.getHeight(); y++)
        for (int x = 0; x < m.getWidth(); x++)
          pixels[y * m.getWidth() + x] = m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
      Result result =
          decoder.decode(new RGBLuminanceSource(m.getWidth(), m.getHeight(), pixels), false);
      assertNotNull(format.name(), result);
      assertEquals(value, result.getText());
    }
  }

  @Test
  public void qrModeDoesNotDecodeRetailBarcodes() throws Exception {
    com.google.zxing.common.BitMatrix m =
        BarcodeGenerator.generate("5901234123457", "EAN_13", 800, 400);
    int[] pixels = new int[m.getWidth() * m.getHeight()];
    for (int y = 0; y < m.getHeight(); y++)
      for (int x = 0; x < m.getWidth(); x++)
        pixels[y * m.getWidth() + x] = m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
    assertNull(
        new BarcodeDecoder()
            .decode(new RGBLuminanceSource(m.getWidth(), m.getHeight(), pixels), true));
  }

  @Test
  public void unsafeUriIsNotMislabelledAsPlainText() {
    Result r = new Result("https://jenga-code.com/", null, null, BarcodeFormat.QR_CODE);
    assertEquals("URL", ContentClassifier.classify(r));
    Result wifi = new Result("WIFI:T:WPA;S:Demo;P:secret;;", null, null, BarcodeFormat.QR_CODE);
    assertEquals("Wi-Fi", ContentClassifier.classify(wifi));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidChecksumRejected() throws Exception {
    BarcodeGenerator.generate("5901234123458", "EAN_13", 800, 400);
  }

  @Test(expected = IllegalArgumentException.class)
  public void excessiveDimensionsRejected() throws Exception {
    BarcodeGenerator.generate("hello", "QR_CODE", 4096, 4096);
  }
}
