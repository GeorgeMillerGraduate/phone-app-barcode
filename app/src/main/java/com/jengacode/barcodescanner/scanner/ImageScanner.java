package com.jengacode.barcodescanner.scanner;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import com.google.zxing.*;
import java.io.*;

public final class ImageScanner {
  private ImageScanner() {}

  public static Result scan(Context c, Uri uri, boolean qr) throws IOException {
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inJustDecodeBounds = true;
    try (InputStream in = c.getContentResolver().openInputStream(uri)) {
      BitmapFactory.decodeStream(in, null, opts);
    }
    if (opts.outWidth <= 0 || opts.outHeight <= 0)
      throw new IOException("Choose a valid PNG or JPEG image");
    opts.inSampleSize = 1;
    while (opts.outWidth / opts.inSampleSize > 2048 || opts.outHeight / opts.inSampleSize > 2048)
      opts.inSampleSize *= 2;
    opts.inJustDecodeBounds = false;
    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
    Bitmap bitmap;
    try (InputStream in = c.getContentResolver().openInputStream(uri)) {
      bitmap = BitmapFactory.decodeStream(in, null, opts);
    }
    if (bitmap == null) throw new IOException("This image could not be opened");
    try {
      Matrix m = new Matrix();
      try (InputStream in = c.getContentResolver().openInputStream(uri)) {
        if (in != null) {
          ExifInterface exif = new ExifInterface(in);
          m.postRotate(exif.getRotationDegrees());
          if (exif.isFlipped()) m.postScale(-1, 1);
        }
      } catch (IOException ignored) {
        /* PNGs may not have EXIF. */
      }
      if (!m.isIdentity()) {
        Bitmap corrected =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        if (corrected != bitmap) {
          bitmap.recycle();
          bitmap = corrected;
        }
      }
      BarcodeDecoder decoder = new BarcodeDecoder();
      for (int i = 0; i < 4; i++) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        Result result = decoder.decode(new RGBLuminanceSource(w, h, pixels), qr);
        if (result != null) return result;
        if (i < 3) {
          Matrix turn = new Matrix();
          turn.postRotate(90);
          Bitmap next = Bitmap.createBitmap(bitmap, 0, 0, w, h, turn, false);
          bitmap.recycle();
          bitmap = next;
        }
      }
      return null;
    } finally {
      bitmap.recycle();
    }
  }
}
