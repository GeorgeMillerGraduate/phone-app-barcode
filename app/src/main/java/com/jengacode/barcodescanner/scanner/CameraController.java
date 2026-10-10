package com.jengacode.barcodescanner.scanner;

import android.content.Context;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.*;

public final class CameraController {
  public interface Listener {
    void ready(Camera camera, boolean canSwitch);

    void error(String message);
  }

  private final Context context;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private ProcessCameraProvider provider;
  private ImageAnalysis analysis;
  private boolean closed;
  private int generation;

  public CameraController(Context c) {
    context = c.getApplicationContext();
  }

  public void bind(
      LifecycleOwner owner,
      PreviewView view,
      int facing,
      ImageAnalysis.Analyzer analyzer,
      Listener listener) {
    int requested = ++generation;
    ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
    future.addListener(
        () -> {
          if (closed || requested != generation) return;
          try {
            provider = future.get();
            provider.unbindAll();
            CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(facing).build();
            if (!provider.hasCamera(selector))
              selector =
                  facing == CameraSelector.LENS_FACING_BACK
                      ? CameraSelector.DEFAULT_FRONT_CAMERA
                      : CameraSelector.DEFAULT_BACK_CAMERA;
            if (!provider.hasCamera(selector)) {
              listener.error("No camera is available. You can still scan an image.");
              return;
            }
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(view.getSurfaceProvider());
            analysis =
                new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(new android.util.Size(1280, 720))
                    .build();
            analysis.setAnalyzer(executor, analyzer);
            Camera camera = provider.bindToLifecycle(owner, selector, preview, analysis);
            camera
                .getCameraInfo()
                .getCameraState()
                .observe(
                    owner,
                    state -> {
                      if (state.getError() != null)
                        listener.error(
                            "Camera unavailable or in use. Close other camera apps, then tap"
                                + " Retry.");
                    });
            listener.ready(
                camera,
                provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    && provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA));
          } catch (Exception e) {
            listener.error("Unable to start the camera. Tap Retry or scan an image.");
          }
        },
        ContextCompat.getMainExecutor(context));
  }

  public void close() {
    closed = true;
    generation++;
    if (analysis != null) analysis.clearAnalyzer();
    if (provider != null) provider.unbindAll();
    executor.shutdown();
  }
}
