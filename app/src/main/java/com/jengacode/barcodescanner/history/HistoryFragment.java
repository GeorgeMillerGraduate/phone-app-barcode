package com.jengacode.barcodescanner.history;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jengacode.barcodescanner.R;
import com.jengacode.barcodescanner.database.*;
import com.jengacode.barcodescanner.scanner.*;
import com.jengacode.barcodescanner.utils.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.*;

public class HistoryFragment extends Fragment {
  private List<ScanRecord> all = new ArrayList<>();
  private final List<ScanRecord> visible = new ArrayList<>();
  private EditText search;
  private Spinner filter, sort;
  private ScanAdapter adapter;
  private final ActivityResultLauncher<String> csv =
      registerForActivityResult(
          new ActivityResultContracts.CreateDocument("text/csv"),
          u -> {
            if (u != null) export(u, false);
          });
  private final ActivityResultLauncher<String> json =
      registerForActivityResult(
          new ActivityResultContracts.CreateDocument("application/json"),
          u -> {
            if (u != null) export(u, true);
          });

  public HistoryFragment() {
    super(R.layout.fragment_history);
  }

  @Override
  public void onViewCreated(View v, Bundle state) {
    search = v.findViewById(R.id.search);
    filter = v.findViewById(R.id.history_format);
    sort = v.findViewById(R.id.sort);
    List<String> formats = new ArrayList<>();
    formats.add("All formats");
    for (com.google.zxing.BarcodeFormat f : BarcodeDecoder.FORMATS) formats.add(f.name());
    filter.setAdapter(
        new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, formats));
    sort.setAdapter(
        new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"Newest first", "Oldest first"}));
    if (state != null) {
      search.setText(state.getString("query"));
      filter.setSelection(state.getInt("filter"));
      sort.setSelection(state.getInt("sort"));
    }
    RecyclerView list = v.findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new ScanAdapter();
    list.setAdapter(adapter);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int n) {}

          public void onTextChanged(CharSequence s, int a, int b, int c) {
            refresh();
          }

          public void afterTextChanged(Editable e) {}
        });
    AdapterView.OnItemSelectedListener changed =
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View view, int n, long id) {
            refresh();
          }
        };
    filter.setOnItemSelectedListener(changed);
    sort.setOnItemSelectedListener(changed);
    v.findViewById(R.id.csv).setOnClickListener(b -> csv.launch("jenga-scan-history.csv"));
    v.findViewById(R.id.json).setOnClickListener(b -> json.launch("jenga-scan-history.json"));
    v.findViewById(R.id.clear).setOnClickListener(b -> confirmClear());
    try {
      ScanDatabase.get(requireContext())
          .scans()
          .observeAll()
          .observe(
              getViewLifecycleOwner(),
              records -> {
                all = records == null ? new ArrayList<>() : records;
                refresh();
              });
    } catch (Exception e) {
      status("History unavailable. Close and reopen the app to retry.");
    }
  }

  private void refresh() {
    if (adapter == null || getView() == null) return;
    String query = search.getText().toString().toLowerCase(Locale.ROOT),
        selected = String.valueOf(filter.getSelectedItem());
    visible.clear();
    for (ScanRecord r : all)
      if ((filter.getSelectedItemPosition() == 0 || r.format.equals(selected))
          && (r.value.toLowerCase(Locale.ROOT).contains(query)
              || r.note.toLowerCase(Locale.ROOT).contains(query))) visible.add(r);
    if (sort.getSelectedItemPosition() == 1) Collections.reverse(visible);
    adapter.notifyDataSetChanged();
    status(
        visible.isEmpty()
            ? (all.isEmpty()
                ? "No scans yet. Save a result from Scanner to start your history."
                : "No scans match this search.")
            : visible.size() + " scans · Exports include all history");
  }

  private void actions(ScanRecord r) {
    Context c = requireContext();
    new MaterialAlertDialogBuilder(c)
        .setTitle(r.format)
        .setItems(
            new String[] {"View result", "Copy", "Share", "Edit note", "Delete"},
            (d, n) -> {
              if (n == 0)
                ResultDialog.create(r.value, r.format, r.timestamp, r.id)
                    .show(getParentFragmentManager(), "history-result");
              else if (n == 1) Actions.copy(c, r.value);
              else if (n == 2) Actions.share(c, r.value);
              else if (n == 3) {
                EditText text = new EditText(c);
                text.setText(r.note);
                text.setHint("Optional note");
                text.setFilters(new InputFilter[] {new InputFilter.LengthFilter(2000)});
                new MaterialAlertDialogBuilder(c)
                    .setTitle("Edit note")
                    .setView(text)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                        "Save",
                        (dialog, w) -> {
                          String note = text.getText().toString();
                          mutate(() -> ScanDatabase.get(c).scans().setNote(r.id, note));
                        })
                    .show();
              } else
                new MaterialAlertDialogBuilder(c)
                    .setTitle("Delete this scan?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                        "Delete",
                        (dialog, w) -> mutate(() -> ScanDatabase.get(c).scans().delete(r)))
                    .show();
            })
        .show();
  }

  private void confirmClear() {
    Context c = requireContext().getApplicationContext();
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle("Clear all history?")
        .setMessage(
            "This permanently deletes all scans and notes on this phone. Export first if you want a"
                + " copy.")
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Clear", (d, w) -> mutate(() -> ScanDatabase.get(c).scans().clear()))
        .show();
  }

  private void mutate(Runnable operation) {
    Tasks.IO.execute(
        () -> {
          try {
            operation.run();
          } catch (Exception e) {
            Tasks.MAIN.post(() -> status("Could not update history. Try again."));
          }
        });
  }

  private void export(Uri uri, boolean asJson) {
    Context c = requireContext().getApplicationContext();
    status("Exporting…");
    Tasks.IO.execute(
        () -> {
          try {
            List<ScanRecord> records = ScanDatabase.get(c).scans().all();
            String content = asJson ? ExportCodec.json(records) : ExportCodec.csv(records);
            try (OutputStream out = c.getContentResolver().openOutputStream(uri)) {
              if (out == null) throw new IOException();
              out.write(content.getBytes(StandardCharsets.UTF_8));
            }
            Tasks.MAIN.post(() -> status("Exported " + records.size() + " scans"));
          } catch (Exception e) {
            Tasks.MAIN.post(() -> status("Export failed. Choose another location and try again."));
          }
        });
  }

  private void status(String s) {
    if (getView() != null) ((TextView) getView().findViewById(R.id.history_status)).setText(s);
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    if (search != null) {
      b.putString("query", search.getText().toString());
      b.putInt("filter", filter.getSelectedItemPosition());
      b.putInt("sort", sort.getSelectedItemPosition());
    }
  }

  private class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.Holder> {
    class Holder extends RecyclerView.ViewHolder {
      Holder(View v) {
        super(v);
      }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup p, int type) {
      return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_scan, p, false));
    }

    @Override
    public int getItemCount() {
      return visible.size();
    }

    @Override
    public void onBindViewHolder(Holder h, int n) {
      ScanRecord r = visible.get(n);
      ((TextView) h.itemView.findViewById(R.id.row_value)).setText(r.value);
      ((TextView) h.itemView.findViewById(R.id.row_details))
          .setText(
              r.format
                  + " · "
                  + r.classification
                  + "\n"
                  + DateFormat.getDateTimeInstance().format(new Date(r.timestamp)));
      TextView note = h.itemView.findViewById(R.id.row_note);
      note.setText(r.note);
      note.setVisibility(r.note.isEmpty() ? View.GONE : View.VISIBLE);
      h.itemView.findViewById(R.id.row_actions).setOnClickListener(v -> actions(r));
    }
  }
}
