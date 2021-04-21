package com.gpaglia.bt.examples.advmon;

import java.util.Arrays;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

public class AdFilter extends Struct {
  @Position(0)
  private final byte position;

  @Position(1)
  private final byte adType;

  @Position(2)
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
