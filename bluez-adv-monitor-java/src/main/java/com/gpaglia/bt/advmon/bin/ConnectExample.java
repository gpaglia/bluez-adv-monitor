package com.gpaglia.bt.advmon.bin;

import java.util.Arrays;
import java.util.List;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectExample {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectExample.class);

  private static final String DEVICE_ADDR = "58:2d:34:32:5a:38";

  private static final String MI_SERVICE = "0000fe95-0000-1000-8000-00805f9b34fb";
  private static final String TOKEN_CHAR = "00000001-0000-1000-8000-00805f9b34fb";
  private static final String EVENT_CHAR = "00000010-0000-1000-8000-00805f9b34fb";
  private static final String VERSION_CHAR = "00000004-0000-1000-8000-00805f9b34fb";

  public static void main(String[] args) {
    DeviceManager dm = null;
    try {
      dm = DeviceManager.createInstance(false);

      final List<BluetoothDevice> devices = dm.scanForBluetoothDevices(20 * 1000);
      final BluetoothDevice device = devices
        .stream()
        .filter(d -> d.getAddress().equalsIgnoreCase(DEVICE_ADDR))
        .findFirst()
        .orElse(null);

      if (device == null) {
        LOGGER.error("Device {} not found", DEVICE_ADDR);
        System.exit(1);
      }

      LOGGER.info("Device {} was found", DEVICE_ADDR);
      if (! device.connect()) {
        LOGGER.error("Could not connect");
        System.exit(1);
      }

      Arrays.stream(device.getUuids()).forEach(uuid -> LOGGER.info("Found uuid {}", uuid));

      List<BluetoothGattService> services = null;
      for (int i = 0; i < 5; i++) {
        LOGGER.info("Retry {} -- Device connected: {}, svc resolved: {}", i, device.isConnected(), device.isServicesResolved());

        device.refreshGattServices();
  
        services = device.getGattServices();
        if (services != null && ! services.isEmpty()) {
          break;
        } 
        
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignored) {
          // no op
        }
      }
      
      if (services == null || services.isEmpty()) {
        LOGGER.error("No services for device {}", DEVICE_ADDR);
        System.exit(1);
      }

      for (BluetoothGattService s : services) {
        LOGGER.info("Found service {}", s.getUuid());
      }

      final BluetoothGattService miService = device.getGattServiceByUuid(MI_SERVICE);
      if (miService == null) {
        LOGGER.error("Mi Service {} not found", MI_SERVICE);
        System.exit(1);
      }

      final BluetoothGattCharacteristic tokenChar = miService.getGattCharacteristicByUuid(TOKEN_CHAR);
      if (tokenChar == null) {
        LOGGER.error("Token Char {} not found", TOKEN_CHAR);
        System.exit(1);
      }

      final BluetoothGattCharacteristic eventChar = miService.getGattCharacteristicByUuid(EVENT_CHAR);
      if (eventChar == null) {
        LOGGER.error("Event Char {} not found", EVENT_CHAR);
        System.exit(1);
      }

      final BluetoothGattCharacteristic versionChar = miService.getGattCharacteristicByUuid(VERSION_CHAR);
      if (versionChar == null) {
        LOGGER.error("Version Char {} not found", VERSION_CHAR);
        System.exit(1);
      }

    } catch (DBusException ex) {
      ex.printStackTrace();
      System.exit(1);
    } finally {
      dm.closeConnection();
    }

  }
  
}
