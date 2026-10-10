package com.jengacode.barcodescanner;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jengacode.barcodescanner.generator.GeneratorFragment;
import com.jengacode.barcodescanner.history.HistoryFragment;
import com.jengacode.barcodescanner.scanner.ScannerFragment;
import com.jengacode.barcodescanner.settings.Preferences;
import com.jengacode.barcodescanner.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle state) {
    Preferences.applyTheme(this);
    super.onCreate(state);
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    setContentView(R.layout.activity_main);
    boolean light =
        (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            != android.content.res.Configuration.UI_MODE_NIGHT_YES;
    androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
        .setAppearanceLightNavigationBars(light);
    if (android.os.Build.VERSION.SDK_INT == 26)
      getWindow()
          .setNavigationBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.navy));
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.root),
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    BottomNavigationView nav = findViewById(R.id.navigation);
    nav.setOnItemSelectedListener(
        item -> {
          Fragment current = getSupportFragmentManager().findFragmentById(R.id.content);
          String tag = String.valueOf(item.getItemId());
          if (current != null && tag.equals(current.getTag())) return true;
          Fragment next;
          if (item.getItemId() == R.id.nav_generate) next = new GeneratorFragment();
          else if (item.getItemId() == R.id.nav_history) next = new HistoryFragment();
          else if (item.getItemId() == R.id.nav_settings) next = new SettingsFragment();
          else next = new ScannerFragment();
          getSupportFragmentManager().beginTransaction().replace(R.id.content, next, tag).commit();
          return true;
        });
    if (state == null) nav.setSelectedItemId(R.id.nav_scanner);
  }
}
