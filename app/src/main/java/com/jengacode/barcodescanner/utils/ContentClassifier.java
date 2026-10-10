package com.jengacode.barcodescanner.utils;

import com.google.zxing.Result;
import com.google.zxing.client.result.*;

public final class ContentClassifier {
  private ContentClassifier() {}

  public static ParsedResult parse(Result result) {
    return ResultParser.parseResult(result);
  }

  public static String classify(Result result) {
    return switch (parse(result).getType()) {
      case URI -> "URL";
      case EMAIL_ADDRESS -> "Email";
      case TEL -> "Telephone";
      case WIFI -> "Wi-Fi";
      case ADDRESSBOOK -> "Contact";
      case PRODUCT, ISBN -> "Product";
      default -> "Text";
    };
  }
}
