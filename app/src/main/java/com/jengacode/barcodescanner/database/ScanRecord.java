package com.jengacode.barcodescanner.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scans")
public class ScanRecord {
  @PrimaryKey(autoGenerate = true)
  public long id;

  @NonNull public String value = "";
  @NonNull public String format = "";
  public long timestamp;
  @NonNull public String classification = "Text";
  @NonNull public String note = "";
}
