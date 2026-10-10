package com.jengacode.barcodescanner.settings;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public final class Preferences {
  private Preferences() {}

  public static SharedPreferences get(Context c) {
    return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
  }

  public static void applyTheme(Context c) {
    String theme = get(c).getString("theme", "Dark");
    AppCompatDelegate.setDefaultNightMode(
        "Light".equals(theme)
            ? AppCompatDelegate.MODE_NIGHT_NO
            : "System".equals(theme)
                ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                : AppCompatDelegate.MODE_NIGHT_YES);
  }
}
