package com.jengacode.barcodescanner.settings;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jengacode.barcodescanner.R;
import com.jengacode.barcodescanner.database.ScanDatabase;
import com.jengacode.barcodescanner.utils.*;

public class SettingsFragment extends Fragment {
  public SettingsFragment() {
    super(R.layout.fragment_settings);
  }

  @Override
  public void onViewCreated(View v, Bundle state) {
    Context c = requireContext();
    SharedPreferences p = Preferences.get(c);
    SwitchMaterial sound = v.findViewById(R.id.sound), vibration = v.findViewById(R.id.vibration);
    sound.setChecked(p.getBoolean("sound", true));
    vibration.setChecked(p.getBoolean("vibration", true));
    sound.setOnCheckedChangeListener((b, checked) -> p.edit().putBoolean("sound", checked).apply());
    vibration.setOnCheckedChangeListener(
        (b, checked) -> p.edit().putBoolean("vibration", checked).apply());
    options(
        v,
        R.id.default_mode,
        new String[] {"Single scan", "Continuous scan", "QR code only"},
        p.getInt("mode", 0),
        n -> p.edit().putInt("mode", n).apply());
    int[] seconds = {2, 5, 10, 30, 60};
    int selected = 1;
    for (int i = 0; i < seconds.length; i++)
      if (seconds[i] == p.getInt("cooldown", 5)) selected = i;
    options(
        v,
        R.id.cooldown,
        new String[] {"2 seconds", "5 seconds", "10 seconds", "30 seconds", "60 seconds"},
        selected,
        n -> p.edit().putInt("cooldown", seconds[n]).apply());
    options(
        v,
        R.id.default_camera,
        new String[] {"Rear camera", "Front camera"},
        p.getInt("camera", 1) == 1 ? 0 : 1,
        n -> p.edit().putInt("camera", n == 0 ? 1 : 0).apply());
    String[] themes = {"Dark", "Light", "System"};
    int theme = 0;
    for (int i = 0; i < themes.length; i++)
      if (themes[i].equals(p.getString("theme", "Dark"))) theme = i;
    options(
        v,
        R.id.theme,
        themes,
        theme,
        n -> {
          if (!themes[n].equals(p.getString("theme", "Dark"))) {
            p.edit().putString("theme", themes[n]).apply();
            Preferences.applyTheme(c);
          }
        });
    v.findViewById(R.id.settings_clear)
        .setOnClickListener(
            b ->
                new MaterialAlertDialogBuilder(c)
                    .setTitle("Clear all scan history?")
                    .setMessage(
                        "This permanently deletes your scans and notes. Export any scans you want"
                            + " to keep first.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                        "Clear",
                        (d, w) -> {
                          Context app = c.getApplicationContext();
                          Tasks.IO.execute(
                              () -> {
                                try {
                                  ScanDatabase.get(app).scans().clear();
                                  Tasks.MAIN.post(() -> Actions.message(app, "History cleared"));
                                } catch (Exception e) {
                                  Tasks.MAIN.post(
                                      () ->
                                          Actions.message(
                                              app, "Could not clear history. Try again."));
                                }
                              });
                        })
                    .show());
    v.findViewById(R.id.privacy)
        .setOnClickListener(
            b ->
                new MaterialAlertDialogBuilder(c)
                    .setTitle("Your scans stay on your phone")
                    .setMessage(
                        "Camera frames are decoded on this device and discarded. Images selected"
                            + " for scanning are processed locally. Saved scans and notes are held"
                            + " in local app storage; cloud backup is disabled.\n\n"
                            + "There are no adverts, analytics, accounts or tracking. No camera"
                            + " images or history are uploaded by this app.\n\n"
                            + "Copy, share, export, website, contact and search actions happen only"
                            + " when you choose them. Other apps and websites have their own"
                            + " privacy policies. Exports saved outside the app remain after"
                            + " history is deleted.\n\n"
                            + "Clear history here or in History. Android Settings > Apps > Storage"
                            + " > Clear data removes settings and history. Uninstalling removes"
                            + " local app data.")
                    .setPositiveButton("Done", null)
                    .show());
    v.findViewById(R.id.about)
        .setOnClickListener(
            b ->
                new MaterialAlertDialogBuilder(c)
                    .setTitle("Jenga Barcode Scanner 1.0")
                    .setMessage(
                        "A native Android application written in Java.\n\n"
                            + "Created for Jenga-Code by George Miller.\n\n"
                            + "CameraX camera · ZXing decoding · Room storage.\n\n"
                            + "Scan, generate and manage codes offline.")
                    .setNegativeButton("Done", null)
                    .setPositiveButton(
                        "Jenga-Code website",
                        (d, w) ->
                            Actions.launch(
                                c,
                                new Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://jenga-code.com/"))))
                    .show());
  }

  private interface Selected {
    void choose(int n);
  }

  private void options(View v, int id, String[] values, int selection, Selected callback) {
    Spinner s = v.findViewById(id);
    s.setAdapter(
        new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, values));
    s.setSelection(selection);
    s.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View view, int n, long row) {
            callback.choose(n);
          }
        });
  }
}
