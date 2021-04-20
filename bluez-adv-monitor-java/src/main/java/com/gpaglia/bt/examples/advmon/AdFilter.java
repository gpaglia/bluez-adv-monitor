package com.gpaglia.bt.examples.advmon;

import java.util.Arrays;

public class AdFilter {
  private final byte position;
  private final byte adType;
  private final byte[] adData;

  public AdFilter(byte position, byte adType, byte[] adData) {
    this.position = position;
    this.adType = adType;
    this.adData = Arrays.copyOf(adData, adData.length);
  }

  public byte getPosition() { return position; }

  public byte getAdType() { return adType; }

  public byte[] getAdData() { return Arrays.copyOf(adData, adData.length); }
}
