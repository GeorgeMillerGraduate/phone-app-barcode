package com.jengacode.barcodescanner.database;

import android.content.Context;
import androidx.room.*;

@Database(
    entities = {ScanRecord.class},
    version = 1,
    exportSchema = true)
public abstract class ScanDatabase extends RoomDatabase {
  public abstract ScanDao scans();

  private static volatile ScanDatabase instance;

  public static ScanDatabase get(Context c) {
    if (instance == null)
      synchronized (ScanDatabase.class) {
        if (instance == null)
          instance =
              Room.databaseBuilder(c.getApplicationContext(), ScanDatabase.class, "jenga-scans.db")
                  .build();
      }
    return instance;
  }
}
