package com.jengacode.barcodescanner.generator;

import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.google.zxing.common.BitMatrix;
import com.jengacode.barcodescanner.R;
import com.jengacode.barcodescanner.utils.*;
import java.io.*;

public class GeneratorFragment extends Fragment {
  private Bitmap bitmap;
  private EditText input, width, height;
  private Spinner format;
  private int request;
  private final ActivityResultLauncher<String> save =
      registerForActivityResult(
          new ActivityResultContracts.CreateDocument("image/png"),
          uri -> {
            if (uri != null && bitmap != null) write(uri, false);
          });

  public GeneratorFragment() {
    super(R.layout.fragment_generator);
  }

  @Override
  public void onViewCreated(View v, Bundle state) {
    input = v.findViewById(R.id.input);
    width = v.findViewById(R.id.width);
    height = v.findViewById(R.id.height);
    format = v.findViewById(R.id.format);
    format.setAdapter(
        new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            BarcodeGenerator.FORMATS));
    if (state != null) {
      input.setText(state.getString("text"));
      width.setText(state.getString("width", "800"));
      height.setText(state.getString("height", "800"));
      format.setSelection(state.getInt("format"));
      if (state.getBoolean("generated")) generate();
    }
    v.findViewById(R.id.create).setOnClickListener(b -> generate());
    v.findViewById(R.id.save_png).setOnClickListener(b -> save.launch("jenga-barcode.png"));
    v.findViewById(R.id.share_png).setOnClickListener(b -> write(null, true));
  }

  private void generate() {
    String value = input.getText().toString(), type = (String) format.getSelectedItem();
    int w, h;
    try {
      w = Integer.parseInt(width.getText().toString());
      h = Integer.parseInt(height.getText().toString());
    } catch (NumberFormatException e) {
      status("Enter numeric width and height");
      return;
    }
    int current = ++request;
    status("Generating…");
    getView().findViewById(R.id.save_png).setEnabled(false);
    getView().findViewById(R.id.share_png).setEnabled(false);
    Tasks.IO.execute(
        () -> {
          try {
            BitMatrix matrix = BarcodeGenerator.generate(value, type, w, h);
            int actualW = matrix.getWidth(), actualH = matrix.getHeight();
            int[] pixels = new int[actualW * actualH];
            for (int y = 0; y < actualH; y++)
              for (int x = 0; x < actualW; x++)
                pixels[y * actualW + x] = matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            Bitmap image = Bitmap.createBitmap(pixels, actualW, actualH, Bitmap.Config.ARGB_8888);
            Tasks.MAIN.post(
                () -> {
                  if (getView() != null && current == request) {
                    bitmap = image;
                    ((ImageView) getView().findViewById(R.id.generated)).setImageBitmap(image);
                    getView().findViewById(R.id.save_png).setEnabled(true);
                    getView().findViewById(R.id.share_png).setEnabled(true);
                    status(type + " · " + actualW + " × " + actualH + " pixels");
                  } else image.recycle();
                });
          } catch (Exception | OutOfMemoryError e) {
            Tasks.MAIN.post(
                () -> {
                  if (getView() != null && current == request)
                    status(
                        "Could not generate: "
                            + (e.getMessage() == null
                                ? "try a shorter value or smaller dimensions"
                                : e.getMessage()));
                });
          }
        });
  }

  private void write(Uri destination, boolean share) {
    Context c = requireContext().getApplicationContext();
    Bitmap image = bitmap;
    status(share ? "Preparing image…" : "Saving PNG…");
    Tasks.IO.execute(
        () -> {
          try {
            Uri target = destination;
            if (share) {
              File dir = new File(c.getCacheDir(), "barcodes");
              if (!dir.exists() && !dir.mkdirs()) throw new IOException();
              File file = new File(dir, "barcode.png");
              try (OutputStream out = new FileOutputStream(file)) {
                if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException();
              }
              target = FileProvider.getUriForFile(c, c.getPackageName() + ".files", file);
            } else {
              try (OutputStream out = c.getContentResolver().openOutputStream(target)) {
                if (out == null || !image.compress(Bitmap.CompressFormat.PNG, 100, out))
                  throw new IOException();
              }
            }
            Uri uri = target;
            Tasks.MAIN.post(
                () -> {
                  if (getView() != null) {
                    status(share ? "Image ready to share" : "PNG saved");
                    if (share) {
                      Intent intent =
                          new Intent(Intent.ACTION_SEND)
                              .setType("image/png")
                              .putExtra(Intent.EXTRA_STREAM, uri)
                              .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                      intent.setClipData(ClipData.newUri(c.getContentResolver(), "Barcode", uri));
                      Actions.launch(
                          requireContext(), Intent.createChooser(intent, "Share barcode image"));
                    }
                  }
                });
          } catch (Exception e) {
            Tasks.MAIN.post(
                () -> {
                  if (getView() != null) status("Could not save image. Choose another location.");
                });
          }
        });
  }

  private void status(String s) {
    if (getView() != null) ((TextView) getView().findViewById(R.id.generator_status)).setText(s);
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    if (input != null) {
      b.putString("text", input.getText().toString());
      b.putString("width", width.getText().toString());
      b.putString("height", height.getText().toString());
      b.putInt("format", format.getSelectedItemPosition());
      b.putBoolean("generated", bitmap != null);
    }
  }

  @Override
  public void onDestroyView() {
    request++;
    bitmap = null;
    super.onDestroyView();
  }
}
