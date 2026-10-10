package com.jengacode.barcodescanner.scanner;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.*;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.Result;
import com.jengacode.barcodescanner.R;
import com.jengacode.barcodescanner.database.*;
import com.jengacode.barcodescanner.settings.Preferences;
import com.jengacode.barcodescanner.utils.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScannerFragment extends Fragment {
  private final AtomicBoolean paused = new AtomicBoolean(false);
  private final DuplicateGate duplicates = new DuplicateGate();
  private final BarcodeDecoder decoder = new BarcodeDecoder();
  private volatile int mode;
  private volatile long cooldown = 5000;
  private int facing;
  private CameraController controller;
  private Camera camera;
  private PreviewView preview;
  private TextView status;
  private Button torch, pause, switchCamera;
  private String lastValue, lastFormat;
  private long lastTime, lastId;
  private final ActivityResultLauncher<String> permission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            if (granted) startCamera();
            else if (getView() != null)
              status.setText("Camera permission was denied. Tap Enable camera or scan an image.");
          });
  private final ActivityResultLauncher<String[]> image =
      registerForActivityResult(
          new ActivityResultContracts.OpenDocument(),
          uri -> {
            if (uri != null) importImage(uri);
          });

  public ScannerFragment() {
    super(R.layout.fragment_scanner);
  }

  @Override
  public void onViewCreated(View view, Bundle state) {
    preview = view.findViewById(R.id.preview);
    status = view.findViewById(R.id.status);
    torch = view.findViewById(R.id.torch);
    pause = view.findViewById(R.id.pause);
    switchCamera = view.findViewById(R.id.switch_camera);
    SharedPreferences prefs = Preferences.get(requireContext());
    mode = state == null ? prefs.getInt("mode", 0) : state.getInt("mode");
    facing =
        state == null
            ? prefs.getInt("camera", CameraSelector.LENS_FACING_BACK)
            : state.getInt("camera");
    cooldown = prefs.getInt("cooldown", 5) * 1000L;
    paused.set(state != null && state.getBoolean("paused"));
    if (state != null) {
      lastValue = state.getString("lastValue");
      lastFormat = state.getString("lastFormat");
      lastTime = state.getLong("lastTime");
      lastId = state.getLong("lastId");
    }
    getParentFragmentManager()
        .setFragmentResultListener(
            "scan-saved",
            getViewLifecycleOwner(),
            (key, saved) -> {
              if (lastTime == saved.getLong("time")
                  && java.util.Objects.equals(lastValue, saved.getString("value")))
                lastId = saved.getLong("id");
            });
    getParentFragmentManager()
        .setFragmentResultListener(
            "scan-resume", getViewLifecycleOwner(), (key, saved) -> setPaused(false));
    Spinner modes = view.findViewById(R.id.mode);
    modes.setAdapter(
        new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"Single scan", "Continuous scan", "QR code only"}));
    modes.setSelection(mode);
    modes.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            if (mode != pos) {
              mode = pos;
              setPaused(false);
            }
          }
        });
    controller = new CameraController(requireContext());
    torch.setEnabled(false);
    switchCamera.setEnabled(false);
    torch.setOnClickListener(
        v -> {
          if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            boolean enabled =
                Integer.valueOf(TorchState.ON)
                    .equals(camera.getCameraInfo().getTorchState().getValue());
            camera.getCameraControl().enableTorch(!enabled);
          }
        });
    switchCamera.setOnClickListener(
        v -> {
          facing =
              facing == CameraSelector.LENS_FACING_BACK
                  ? CameraSelector.LENS_FACING_FRONT
                  : CameraSelector.LENS_FACING_BACK;
          startCamera();
        });
    pause.setOnClickListener(v -> setPaused(!paused.get()));
    view.findViewById(R.id.import_image)
        .setOnClickListener(v -> image.launch(new String[] {"image/*"}));
    view.findViewById(R.id.permission).setOnClickListener(v -> requestCamera());
    view.findViewById(R.id.last_result).setOnClickListener(v -> showResult());
    if (lastValue != null) view.findViewById(R.id.last_result).setVisibility(View.VISIBLE);
    preview.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
            v.performClick();
            if (camera != null)
              camera
                  .getCameraControl()
                  .startFocusAndMetering(
                      new FocusMeteringAction.Builder(
                              preview
                                  .getMeteringPointFactory()
                                  .createPoint(event.getX(), event.getY()))
                          .build());
            return true;
          }
          return true;
        });
    SeekBar zoom = view.findViewById(R.id.zoom);
    zoom.setEnabled(false);
    zoom.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}

          public void onProgressChanged(SeekBar b, int n, boolean user) {
            if (user && camera != null) camera.getCameraControl().setLinearZoom(n / 100f);
          }
        });
    pause.setText(paused.get() ? "Resume" : "Pause");
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) startCamera();
    else if (!prefs.getBoolean("askedCamera", false)) requestCamera();
    else status.setText("Enable camera permission to scan, or choose an image.");
  }

  private void requestCamera() {
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      startCamera();
      return;
    }
    SharedPreferences p = Preferences.get(requireContext());
    if (p.getBoolean("askedCamera", false)
        && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle("Enable camera in Settings")
          .setMessage("Allow Camera under Permissions, then return to the app.")
          .setNegativeButton("Cancel", null)
          .setPositiveButton(
              "Settings",
              (d, w) ->
                  Actions.launch(
                      requireContext(),
                      new Intent(
                          Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                          Uri.parse("package:" + requireContext().getPackageName()))))
          .show();
    } else {
      p.edit().putBoolean("askedCamera", true).apply();
      permission.launch(Manifest.permission.CAMERA);
    }
  }

  private void startCamera() {
    if (getView() == null || controller == null) return;
    status.setText("Camera is starting…");
    controller.bind(
        getViewLifecycleOwner(),
        preview,
        facing,
        proxy -> {
          try {
            if (paused.get()) return;
            Result result = decoder.frame(proxy, mode == 2);
            if (result != null) {
              int scanMode = mode;
              if (scanMode == 1) {
                if (!duplicates.accept(
                    result.getBarcodeFormat() + ":" + result.getText(),
                    SystemClock.elapsedRealtime(),
                    cooldown)) return;
              } else if (!paused.compareAndSet(false, true)) return;
              Tasks.MAIN.post(
                  () -> {
                    if (getView() != null && isResumed()) detected(result, scanMode);
                  });
            }
          } catch (RuntimeException ignored) {
            /* A malformed frame is skipped; the next frame can recover. */
          } finally {
            proxy.close();
          }
        },
        new CameraController.Listener() {
          public void ready(Camera ready, boolean canSwitch) {
            if (getView() == null) return;
            camera = ready;
            torch.setEnabled(camera.getCameraInfo().hasFlashUnit());
            switchCamera.setEnabled(canSwitch);
            camera
                .getCameraInfo()
                .getTorchState()
                .observe(
                    getViewLifecycleOwner(),
                    n -> torch.setText(n == TorchState.ON ? "Torch off" : "Torch"));
            camera
                .getCameraInfo()
                .getZoomState()
                .observe(
                    getViewLifecycleOwner(),
                    z -> {
                      if (getView() != null && z != null) {
                        SeekBar slider = getView().findViewById(R.id.zoom);
                        slider.setEnabled(z.getMaxZoomRatio() > z.getMinZoomRatio());
                        slider.setProgress(Math.round(z.getLinearZoom() * 100));
                      }
                    });
            status.setText(
                paused.get()
                    ? "Paused. Tap Resume to scan again."
                    : "Ready. Point at a barcode or QR code.");
          }

          public void error(String text) {
            if (getView() != null) status.setText(text);
          }
        });
  }

  private void setPaused(boolean value) {
    paused.set(value);
    if (getView() != null) {
      pause.setText(value ? "Resume" : "Pause");
      status.setText(
          value ? "Paused. Tap Resume to scan again." : "Ready. Point at a barcode or QR code.");
    }
  }

  private void detected(Result result, int scanMode) {
    lastValue = result.getText();
    lastFormat = result.getBarcodeFormat().name();
    lastTime = System.currentTimeMillis();
    lastId = 0;
    getView().findViewById(R.id.last_result).setVisibility(View.VISIBLE);
    Context context = requireContext().getApplicationContext();
    SharedPreferences p = Preferences.get(context);
    if (p.getBoolean("vibration", true)) {
      android.os.Vibrator vibrator =
          (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
      if (vibrator != null && vibrator.hasVibrator())
        vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
    }
    if (p.getBoolean("sound", true)) {
      android.media.ToneGenerator tone =
          new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 60);
      tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100);
      Tasks.MAIN.postDelayed(tone::release, 250);
    }
    if (scanMode == 1) {
      ScanRecord record = new ScanRecord();
      record.value = lastValue;
      record.format = lastFormat;
      record.timestamp = lastTime;
      record.classification = ContentClassifier.classify(result);
      status.setText("Detected " + lastFormat + ". Saving to history…");
      Tasks.IO.execute(
          () -> {
            try {
              long id = ScanDatabase.get(context).scans().insert(record);
              Tasks.MAIN.post(
                  () -> {
                    if (getView() != null) {
                      if (lastTime == record.timestamp) lastId = id;
                      status.setText(
                          "Saved "
                              + record.format
                              + " · "
                              + (record.value.length() > 80
                                  ? record.value.substring(0, 80) + "…"
                                  : record.value));
                    }
                  });
            } catch (Exception e) {
              Tasks.MAIN.post(
                  () -> {
                    if (getView() != null)
                      status.setText(
                          "Could not save scan. Pause and use View last result to retry.");
                  });
            }
          });
    } else {
      setPaused(true);
      showResult();
    }
  }

  private void showResult() {
    if (lastValue != null && getParentFragmentManager().findFragmentByTag("scan-result") == null) {
      setPaused(true);
      ResultDialog dialog = ResultDialog.create(lastValue, lastFormat, lastTime, lastId);
      dialog.requireArguments().putBoolean("scanner", true);
      dialog.show(getParentFragmentManager(), "scan-result");
    }
  }

  private void importImage(Uri uri) {
    if (getView() == null) return;
    setPaused(true);
    status.setText("Reading image…");
    Context c = requireContext().getApplicationContext();
    boolean qr = mode == 2;
    Tasks.IO.execute(
        () -> {
          try {
            Result r = ImageScanner.scan(c, uri, qr);
            Tasks.MAIN.post(
                () -> {
                  if (getView() != null) {
                    if (r == null)
                      status.setText("No barcode found. Try a clearer image, or tap Resume.");
                    else detected(r, 0);
                  }
                });
          } catch (Exception | OutOfMemoryError e) {
            Tasks.MAIN.post(
                () -> {
                  if (getView() != null)
                    status.setText("Could not read this image. Choose a smaller PNG or JPEG.");
                });
          }
        });
  }

  @Override
  public void onResume() {
    super.onResume();
    if (getView() != null
        && controller != null
        && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        && camera == null) startCamera();
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("mode", mode);
    b.putInt("camera", facing);
    b.putBoolean("paused", paused.get());
    b.putString("lastValue", lastValue);
    b.putString("lastFormat", lastFormat);
    b.putLong("lastTime", lastTime);
    b.putLong("lastId", lastId);
  }

  @Override
  public void onDestroyView() {
    if (controller != null) controller.close();
    controller = null;
    camera = null;
    super.onDestroyView();
  }
}
