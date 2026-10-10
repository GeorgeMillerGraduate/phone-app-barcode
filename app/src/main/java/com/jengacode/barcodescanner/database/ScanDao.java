package com.jengacode.barcodescanner.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface ScanDao {
  @Query("SELECT * FROM scans ORDER BY timestamp DESC, id DESC")
  LiveData<List<ScanRecord>> observeAll();

  @Query("SELECT * FROM scans ORDER BY timestamp DESC, id DESC")
  List<ScanRecord> all();

  @Insert
  long insert(ScanRecord scan);

  @Delete
  void delete(ScanRecord scan);

  @Query("DELETE FROM scans")
  void clear();

  @Query("UPDATE scans SET note = :note WHERE id = :id")
  void setNote(long id, String note);
}
