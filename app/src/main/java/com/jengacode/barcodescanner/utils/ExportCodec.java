package com.jengacode.barcodescanner.utils;

import com.jengacode.barcodescanner.database.ScanRecord;
import java.util.List;

public final class ExportCodec {
  private ExportCodec() {}

  // Neutralise spreadsheet formulas in every text field, including notes.
  public static String csvCell(String s) {
    String t = s.trim();
    if (!t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }

  private static String json(String s) {
    StringBuilder b = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      if (c == '"' || c == '\\') b.append('\\').append(c);
      else if (c < 32) b.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
      else b.append(c);
    }
    return b.append('"').toString();
  }

  public static String csv(List<ScanRecord> records) {
    StringBuilder b = new StringBuilder("id,value,format,timestamp_ms,classification,note\r\n");
    for (ScanRecord r : records)
      b.append(r.id)
          .append(',')
          .append(csvCell(r.value))
          .append(',')
          .append(csvCell(r.format))
          .append(',')
          .append(r.timestamp)
          .append(',')
          .append(csvCell(r.classification))
          .append(',')
          .append(csvCell(r.note))
          .append("\r\n");
    return b.toString();
  }

  public static String json(List<ScanRecord> records) {
    StringBuilder b = new StringBuilder("[\n");
    for (int i = 0; i < records.size(); i++) {
      ScanRecord r = records.get(i);
      if (i > 0) b.append(",\n");
      b.append("{\"id\":")
          .append(r.id)
          .append(",\"value\":")
          .append(json(r.value))
          .append(",\"format\":")
          .append(json(r.format))
          .append(",\"timestamp_ms\":")
          .append(r.timestamp)
          .append(",\"classification\":")
          .append(json(r.classification))
          .append(",\"note\":")
          .append(json(r.note))
          .append('}');
    }
    return b.append("\n]\n").toString();
  }
}
