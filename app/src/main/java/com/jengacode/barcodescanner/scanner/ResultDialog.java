package com.jengacode.barcodescanner.scanner;

import android.app.Dialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.*;
import com.google.zxing.client.result.*;
import com.jengacode.barcodescanner.database.*;
import com.jengacode.barcodescanner.utils.*;
import java.text.DateFormat;
import java.util.Date;

public class ResultDialog extends DialogFragment {
  public static ResultDialog create(String value, String format, long timestamp, long id) {
    ResultDialog d = new ResultDialog();
    Bundle b = new Bundle();
    b.putString("value", value);
    b.putString("format", format);
    b.putLong("time", timestamp);
    b.putLong("id", id);
    d.setArguments(b);
    return d;
  }

  private Result result() {
    Bundle b = requireArguments();
    return new Result(
        b.getString("value"), null, null, BarcodeFormat.valueOf(b.getString("format")));
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle state) {
    Bundle args = requireArguments();
    Context c = requireContext();
    long savedId =
        state == null ? args.getLong("id") : state.getLong("savedId", args.getLong("id"));
    args.putLong("id", savedId);
    LinearLayout box = new LinearLayout(c);
    box.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (20 * getResources().getDisplayMetrics().density);
    box.setPadding(pad, 0, pad, 0);
    TextView details = new TextView(c);
    details.setTextIsSelectable(true);
    details.setText(
        args.getString("format")
            + " · "
            + ContentClassifier.classify(result())
            + "\n"
            + DateFormat.getDateTimeInstance().format(new Date(args.getLong("time")))
            + "\n\n"
            + args.getString("value"));
    box.addView(details);
    button(box, "Copy", () -> Actions.copy(c, args.getString("value")));
    button(box, "Share", () -> Actions.share(c, args.getString("value")));
    button(
        box,
        "Search online",
        () ->
            new MaterialAlertDialogBuilder(c)
                .setTitle("Search online?")
                .setMessage("This sends the scanned text to your browser and search provider.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Search", (d, w) -> Actions.search(c, args.getString("value")))
                .show());
    addContentAction(box, c);
    Button save = button(box, savedId > 0 ? "Saved to history" : "Save to history", () -> {});
    save.setEnabled(savedId == 0);
    save.setOnClickListener(
        v -> {
          save.setEnabled(false);
          ScanRecord r = new ScanRecord();
          r.value = args.getString("value");
          r.format = args.getString("format");
          r.timestamp = args.getLong("time");
          r.classification = ContentClassifier.classify(result());
          Tasks.IO.execute(
              () -> {
                try {
                  long id = ScanDatabase.get(c).scans().insert(r);
                  Tasks.MAIN.post(
                      () -> {
                        args.putLong("id", id);
                        if (isAdded()) {
                          save.setText("Saved to history");
                          Bundle saved = new Bundle();
                          saved.putString("value", r.value);
                          saved.putLong("time", r.timestamp);
                          saved.putLong("id", id);
                          getParentFragmentManager().setFragmentResult("scan-saved", saved);
                        }
                      });
                } catch (Exception e) {
                  Tasks.MAIN.post(
                      () -> {
                        if (isAdded()) {
                          save.setEnabled(true);
                          Actions.message(c, "Could not save. Try again.");
                        }
                      });
                }
              });
        });
    ScrollView scroll = new ScrollView(c);
    scroll.addView(box);
    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(c)
            .setTitle("Scan result")
            .setView(scroll)
            .setPositiveButton("Done", (d, w) -> {});
    if (args.getBoolean("scanner"))
      builder.setNegativeButton(
          "Scan again",
          (d, w) -> getParentFragmentManager().setFragmentResult("scan-resume", new Bundle()));
    return builder.create();
  }

  private Button button(LinearLayout box, String title, Runnable action) {
    Button b = new com.google.android.material.button.MaterialButton(requireContext());
    b.setText(title);
    b.setOnClickListener(v -> action.run());
    box.addView(b);
    return b;
  }

  private void addContentAction(LinearLayout box, Context c) {
    ParsedResult parsed = ContentClassifier.parse(result());
    if (parsed instanceof URIParsedResult uri) {
      Uri target = Uri.parse(uri.getURI());
      String scheme = target.getScheme();
      if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
        button(
            box,
            "Open website…",
            () ->
                confirm(
                    c, "Open this website?", uri.getURI(), new Intent(Intent.ACTION_VIEW, target)));
    } else if (parsed instanceof TelParsedResult tel)
      button(
          box,
          "Open dialler…",
          () ->
              confirm(
                  c,
                  "Open dialler?",
                  tel.getNumber(),
                  new Intent(Intent.ACTION_DIAL, Uri.parse(tel.getTelURI()))));
    else if (parsed instanceof EmailAddressParsedResult email)
      button(
          box,
          "Compose email…",
          () ->
              confirm(
                  c,
                  "Compose an email?",
                  parsed.getDisplayResult(),
                  new Intent(Intent.ACTION_SENDTO, Uri.parse(email.getMailtoURI()))));
    else if (parsed instanceof WifiParsedResult wifi)
      button(
          box,
          "View Wi-Fi details",
          () ->
              new MaterialAlertDialogBuilder(c)
                  .setTitle("Wi-Fi information")
                  .setMessage(
                      "Network: "
                          + wifi.getSsid()
                          + "\nSecurity: "
                          + wifi.getNetworkEncryption()
                          + "\nPassword: "
                          + wifi.getPassword()
                          + "\n\nUse Android Settings to connect manually.")
                  .setPositiveButton("Done", null)
                  .show());
    else if (parsed instanceof AddressBookParsedResult contact)
      button(
          box,
          "Add contact…",
          () -> {
            Intent i =
                new Intent(ContactsContract.Intents.Insert.ACTION)
                    .setType(ContactsContract.RawContacts.CONTENT_TYPE);
            if (contact.getNames() != null && contact.getNames().length > 0)
              i.putExtra(ContactsContract.Intents.Insert.NAME, contact.getNames()[0]);
            if (contact.getPhoneNumbers() != null && contact.getPhoneNumbers().length > 0)
              i.putExtra(ContactsContract.Intents.Insert.PHONE, contact.getPhoneNumbers()[0]);
            if (contact.getEmails() != null && contact.getEmails().length > 0)
              i.putExtra(ContactsContract.Intents.Insert.EMAIL, contact.getEmails()[0]);
            confirm(c, "Open contact editor?", parsed.getDisplayResult(), i);
          });
  }

  private void confirm(Context c, String title, String text, Intent intent) {
    new MaterialAlertDialogBuilder(c)
        .setTitle(title)
        .setMessage(text)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Continue", (d, w) -> Actions.launch(c, intent))
        .show();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out);
    out.putLong("savedId", requireArguments().getLong("id"));
  }
}
