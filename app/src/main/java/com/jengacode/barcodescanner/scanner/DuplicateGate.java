package com.jengacode.barcodescanner.scanner;

import java.util.*;

public final class DuplicateGate {
  private final LinkedHashMap<String, Long> times = new LinkedHashMap<>();

  public synchronized boolean accept(String key, long now, long cooldown) {
    Long previous = times.get(key);
    if (previous != null && now - previous < cooldown) return false;
    times.remove(key);
    times.put(key, now);
    if (times.size() > 256) times.remove(times.keySet().iterator().next());
    return true;
  }
}
