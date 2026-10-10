package com.jengacode.barcodescanner.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Tasks {
  public static final ExecutorService IO = Executors.newSingleThreadExecutor();
  public static final Handler MAIN = new Handler(Looper.getMainLooper());

  private Tasks() {}
}
