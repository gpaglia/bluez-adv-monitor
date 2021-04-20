package com.gpaglia.bt.examples.advmon;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

public interface Commons {
  String DBUS_OM_INTERFACE = "org.freedesktop.DBus.ObjectManager";
  String DBUS_PROPERTIES_INTERFACE = "org.freedesktop.DBus.Properties";
  String BLUEZ_SERVICE_NAME = "org.bluez";

  String ADV_MONITOR_MANAGER_IFACE = "org.bluez.AdvertisementMonitorManager1";
  String ADV_MONITOR_IFACE = "org.bluez.AdvertisementMonitor1";
  String ADV_MONITOR_APP_BASE_PATH = "/org/bluez/example/adv_monitor_app";

  static String appPath(final int appId) { return ADV_MONITOR_APP_BASE_PATH + appId; }

  static String monitorPath(final int appId, final int monId) { return appPath(appId) + "/" + monId; }

  @DBusInterfaceName(ADV_MONITOR_IFACE)
  public interface AdvertisementMonitor1 extends  DBusInterface {
    void Release();
    void Activate();
    void DeviceFound(DBusInterface device);
    void DeviceLost(DBusInterface device);
  }

  @DBusInterfaceName(ADV_MONITOR_MANAGER_IFACE)
  public interface AdvertisementMonitorManager1 extends DBusInterface {
    void RegisterMonitor(DBusPath applicationPath);
    void UnregisterMonitor(DBusPath applicationPath);
  }
}
