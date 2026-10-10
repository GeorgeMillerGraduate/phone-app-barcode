package com.jengacode.barcodescanner.utils;

import android.content.*;
import android.net.Uri;
import android.widget.Toast;

public final class Actions {
  private Actions() {}

  public static void message(Context c, String s) {
    Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
  }

  public static void copy(Context c, String s) {
    ((android.content.ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE))
        .setPrimaryClip(ClipData.newPlainText("Barcode", s));
    message(c, "Copied to clipboard");
  }

  public static void launch(Context c, Intent i) {
    try {
      c.startActivity(i);
    } catch (ActivityNotFoundException | SecurityException e) {
      message(c, "No application is available for this action");
    }
  }

  public static void share(Context c, String s) {
    launch(
        c,
        Intent.createChooser(
            new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, s),
            "Share barcode"));
  }

  public static void search(Context c, String s) {
    launch(
        c,
        new Intent(
            Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(s))));
  }
}
